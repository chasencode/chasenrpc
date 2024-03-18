package consuemer;

import api.RpcContext;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import demo.api.RpcRequest;
import demo.api.RpcResponse;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.*;
import util.MethodUtils;
import util.TypeUtils;

public class ChasenInvocationHandler implements InvocationHandler {

    final static MediaType JSONTYPE = MediaType.get("application/json; charset=utf-8");

    private Class<?> service;

    private RpcContext context;
    private List<String> providers;

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

        RpcResponse rpcResponse = post(request, url);
        if (rpcResponse.isStatus()) {
            Object data = rpcResponse.getData();
            if (data instanceof JSONObject jsonObject) {
                System.out.printf("进入 jsonObject");
                return jsonObject.toJavaObject(method.getReturnType());
            } else if (data instanceof JSONArray jsonArray) {
                Object[] array = jsonArray.toArray();
                Class<?> componentType = method.getReturnType().getComponentType();
                Object resultArray = Array.newInstance(componentType, array.length);
                for (int i = 0; i < array.length; i++) {
                    Array.set(resultArray, i, array[i]);
                }
                return resultArray;
//                return array;
            } else {
                System.out.printf("进入 cast");
                return TypeUtils.cast(rpcResponse.getData(), method.getReturnType());
            }
//            JSONObject data = (JSONObject) rpcResponse.getData();

        } else {
            Exception ex = rpcResponse.getEx();
//            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
//        return null;
    }
    // 可以改为 Gson
    private RpcResponse post(RpcRequest rpcRequest, String url) {

        try {
            String requestJson = JSON.toJSONString(rpcRequest);
            System.out.println(" ===> reqJson = " + requestJson);
            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestJson, JSONTYPE))
                    .build();
            String respJson = client.newCall(request).execute().body().string();
            System.out.println(" ===> respJson = " + respJson);
            return JSON.parseObject(respJson, RpcResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }


    OkHttpClient client = new OkHttpClient().newBuilder()
            .connectionPool(new ConnectionPool(16, 60, TimeUnit.SECONDS))
            .readTimeout(1, TimeUnit.SECONDS)
            .writeTimeout(1, TimeUnit.SECONDS)
            .connectTimeout(1, TimeUnit.SECONDS)
            .build();
    // OkHttpClient
    // 用UrlConnect
    // 用Httpclient
}
