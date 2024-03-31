package cn.chasen.rpc.core.consuemer;

import cn.chasen.rpc.core.api.Filter;
import cn.chasen.rpc.core.api.RpcContext;
import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;
import cn.chasen.rpc.core.meta.InstanceMeta;
import cn.chasen.rpc.core.util.SlidingTimeWindow;
import cn.chasen.rpc.core.consuemer.http.HttpInvoker;
import cn.chasen.rpc.core.consuemer.http.OkHttpInvoker;
import lombok.extern.slf4j.Slf4j;
import cn.chasen.rpc.core.util.MethodUtils;
import cn.chasen.rpc.core.util.TypeUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 消费端动态代理了
 */
@Slf4j
public class RpcInvocationHandler implements InvocationHandler {


    private Class<?> service;

    private RpcContext context;
    private final List<InstanceMeta> providers;

    private List<InstanceMeta> isolatedProviders = new ArrayList<>();
    private List<InstanceMeta> halfOpenProviders = new ArrayList<>();
    HttpInvoker httpInvoker = new OkHttpInvoker();

    Map<String, SlidingTimeWindow> windows = new HashMap<>();

    ScheduledExecutorService executor;

    public RpcInvocationHandler(Class<?> clazz, RpcContext context, List<InstanceMeta> providers) {
        this.service = clazz;
        this.context = context;
        this.providers = providers;
        this.executor = Executors.newScheduledThreadPool(1);
        this.executor.scheduleWithFixedDelay(this::halfOpen, 10, 60, TimeUnit.SECONDS);
    }

    private void halfOpen() {
        log.debug("===> half open");
        halfOpenProviders.clear();
        halfOpenProviders.addAll(isolatedProviders);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        RpcRequest request = new RpcRequest();
        request.setService(service.getCanonicalName());
        request.setMethodSign(MethodUtils.methodSign(method));
        request.setArgs(args);

        for (Filter filter : this.context.getFilters()) {
            Object preResult = filter.prefilter(request);
            if (preResult != null) {
                log.debug(filter.getClass().getName() + " ==> prefilter: " + preResult);
                return preResult;
            }
        }
        InstanceMeta instanceMeta;
        synchronized (halfOpenProviders) {
            if (halfOpenProviders.isEmpty()) {
                List<InstanceMeta> instanceMetaList = context.getRouter().route(providers);
                instanceMeta = context.getLoadBalancer().choose(instanceMetaList);
                log.debug("loadBalancer.choose(urls) ==> {}", instanceMeta);
            } else {
                instanceMeta = halfOpenProviders.remove(0);
                log.debug("check alive instance ===> {}", instanceMeta);
            }
        }

        String url = instanceMeta.toUrl();

        try {
            RpcResponse<?> rpcResponse = httpInvoker.post(request, url);
            Object result = castReturnResult(method, rpcResponse);
//            for (Filter filter : this.context.getFilters()) {
//                Object filterResult = filter.postfilter(request, rpcResponse, result);
//                if(filterResult != null) {
//                    return filterResult;
//                }
//            }
            return result;
        } catch (Exception e) {
            SlidingTimeWindow window = windows.computeIfAbsent(url, k -> new SlidingTimeWindow());
            window.record(System.currentTimeMillis());
            if (window.getSum() >= 10) {
                isolate(instanceMeta);
            }

            synchronized ((providers)) {
                if (!providers.contains(instanceMeta)) {
                    isolatedProviders.remove(instanceMeta);
                    providers.add(instanceMeta);
                    log.debug("instance {} is recovered, isolatedProviders={}, providers={}", isolatedProviders, providers);
                }
            }

            return null;
        }


    }

    private void isolate(InstanceMeta instanceMeta) {
        log.debug(" ===> isolate instance:" + instanceMeta);
        providers.remove(instanceMeta);
        log.debug(" ===> providers = {}", providers);
        isolatedProviders.add(instanceMeta);
        log.debug(" ===> isolatedProviders = {}", isolatedProviders);
    }


    private static Object castReturnResult(Method method, RpcResponse<?> rpcResponse) {
        if (rpcResponse.isStatus()) {
            Object data = rpcResponse.getData();
            return TypeUtils.castMethodResult(method, data);
        } else {
            Exception ex = rpcResponse.getEx();
            throw new RuntimeException(ex);
        }
    }
}
