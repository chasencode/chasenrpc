package consuemer;

import api.RpcContext;
import consuemer.http.HttpInvoker;
import consuemer.http.OkHttpInvoker;
import demo.api.RpcRequest;
import demo.api.RpcResponse;
import util.MethodUtils;
import util.TypeUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * 消费端动态代理了
 */
public class ChasenInvocationHandler implements InvocationHandler {


    private Class<?> service;

    private RpcContext context;
    private List<String> providers;
    HttpInvoker httpInvoker = new OkHttpInvoker();

    public ChasenInvocationHandler(Class<?> clazz, RpcContext context, List<String> providers) {
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

        List<String> urls = context.getRouter().route(providers);
        String url = (String) context.getLoadBalancer().choose(urls);
        System.out.println("loadBalancer.choose(urls) ==> " + url);
        RpcResponse<?> rpcResponse = httpInvoker.post(request, url);
        if (rpcResponse.isStatus()) {
            Object data = rpcResponse.getData();
            return TypeUtils.castMethodResult(method, data);
        } else {
            Exception ex = rpcResponse.getEx();
            //ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
