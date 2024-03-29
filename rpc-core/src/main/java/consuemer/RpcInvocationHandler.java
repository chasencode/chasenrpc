package consuemer;

import api.Filter;
import api.RpcContext;
import consuemer.http.HttpInvoker;
import consuemer.http.OkHttpInvoker;
import api.RpcRequest;
import api.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import meta.InstanceMeta;
import util.MethodUtils;
import util.TypeUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 消费端动态代理了
 */
@Slf4j
public class RpcInvocationHandler implements InvocationHandler {


    private Class<?> service;

    private RpcContext context;
    private List<InstanceMeta> providers;
    HttpInvoker httpInvoker = new OkHttpInvoker();

    public RpcInvocationHandler(Class<?> clazz, RpcContext context, List<InstanceMeta> providers) {
        this.service = clazz;
        this.context = context;
        this.providers = providers;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        RpcRequest request = new RpcRequest();
        request.setService(service.getCanonicalName());
        request.setMethodSign(MethodUtils.methodSign(method));
        request.setArgs(args);

        for (Filter filter : this.context.getFilters()) {
            Object preResult = filter.prefilter(request);
            if(preResult != null) {
                log.debug(filter.getClass().getName() + " ==> prefilter: " + preResult);
                return preResult;
            }
        }

        List<InstanceMeta> instanceMetaList = context.getRouter().route(providers);
        InstanceMeta InstanceMeta = context.getLoadBalancer().choose(instanceMetaList);
        String url = InstanceMeta.toUrl();
        log.debug("loadBalancer.choose(urls) ==> " + url);
        RpcResponse<?> rpcResponse = httpInvoker.post(request, url);
        Object result = castReturnResult(method, rpcResponse);
        for (Filter filter : this.context.getFilters()) {
            Object filterResult = filter.postfilter(request, rpcResponse, result);
            if(filterResult != null) {
                return filterResult;
            }
        }
        return result;
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
