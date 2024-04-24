package cn.chasen.rpc.core.consuemer;

import cn.chasen.rpc.core.api.Filter;
import cn.chasen.rpc.core.api.RpcContext;
import cn.chasen.rpc.core.api.RpcRequest;
import cn.chasen.rpc.core.api.RpcResponse;
import cn.chasen.rpc.core.exception.RpcException;
import cn.chasen.rpc.core.meta.InstanceMeta;
import cn.chasen.rpc.core.util.SlidingTimeWindow;
import cn.chasen.rpc.core.consuemer.http.HttpInvoker;
import cn.chasen.rpc.core.consuemer.http.OkHttpInvoker;
import lombok.extern.slf4j.Slf4j;
import cn.chasen.rpc.core.util.MethodUtils;
import cn.chasen.rpc.core.util.TypeUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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
    HttpInvoker httpInvoker = new OkHttpInvoker(500);

    Map<String, SlidingTimeWindow> windows = new HashMap<>();

    ScheduledExecutorService executor;

    public RpcInvocationHandler(Class<?> clazz, RpcContext context, List<InstanceMeta> providers) {
        this.service = clazz;
        this.context = context;
        this.providers = providers;
        this.executor = Executors.newScheduledThreadPool(1);
        int halfOpenInitialDelay = context.getConsumerProperties().getHalfOpenInitialDelay();
        int halfOpenDelay = context.getConsumerProperties().getHalfOpenDelay();
        this.executor.scheduleWithFixedDelay(this::halfOpen, halfOpenInitialDelay,
                halfOpenDelay, TimeUnit.MILLISECONDS);
    }

    private void halfOpen() {
        log.debug("===> half open");
        halfOpenProviders.clear();
        halfOpenProviders.addAll(isolatedProviders);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {


        if (MethodUtils.checkLocalMethod(method.getName())) {
            return null;
        }

        RpcRequest request = new RpcRequest();
        request.setService(service.getCanonicalName());
        request.setMethodSign(MethodUtils.methodSign(method));
        request.setArgs(args);

        int retries =  context.getConsumerProperties().getRetries();
        int faultLimit = context.getConsumerProperties().getFaultLimit();

        while (retries --> 0) {
            try {
                log.info(" ===> reties: " + retries);
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
                RpcResponse<?> rpcResponse;
                Object result;

                String url = instanceMeta.toUrl();

                try {
                    rpcResponse = httpInvoker.post(request, url);
                    result = castReturnResult(method, rpcResponse);
                } catch (Exception e) {
                    // 故障的规则统计和隔离，
                    // 每一次异常，记录一次，统计30s的异常数。
                    synchronized (windows) {
                        SlidingTimeWindow window = windows.computeIfAbsent(url, k -> new SlidingTimeWindow());
                        window.record(System.currentTimeMillis());
                        log.debug("instance {} in window with {}", url, window.getSum());
                        if (window.getSum() >= faultLimit) {
                            isolate(instanceMeta);
                        }
                    }
                    throw e;
                }

                synchronized ((providers)) {
                    if (!providers.contains(instanceMeta)) {
                        isolatedProviders.remove(instanceMeta);
                        providers.add(instanceMeta);
                        log.debug("instance {} is recovered, isolatedProviders={}, providers={}",instanceMeta, isolatedProviders, providers);
                    }
                }


                for (Filter filter : this.context.getFilters()) {
                    Object filterResult = filter.postfilter(request, rpcResponse, result);
                    if(filterResult != null) {
                        return filterResult;
                    }
                }

            } catch (RuntimeException ex) {
                if (!(ex.getCause() instanceof SocketTimeoutException)) {
                    throw ex;
                }
            }
        }
        return null;

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
            return TypeUtils.castMethodResult(method, rpcResponse.getData());
        } else {
            RpcException exception = rpcResponse.getEx();
            if(exception != null) {
                log.error("response error.", exception);
                throw exception;
            }
            return null;
        }
    }
}
