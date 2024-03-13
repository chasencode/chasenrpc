package consuemer;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import demo.api.RpcRequest;
import demo.api.RpcResponse;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.MediaType;
import okhttp3.*;
import util.MethodUtils;

public class ChasenInvocationHandler implements InvocationHandler {

    final static MediaType JSONTYPE = MediaType.get("application/json; charset=utf-8");

    private Class<?> service;

    public ChasenInvocationHandler(Class<?> clazz) {
        this.service = clazz;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        RpcRequest request = new RpcRequest();
        request.setService(service.getCanonicalName());
        request.setMethodSign(MethodUtils.methodSign(method));
        request.setArgs(args);

        RpcResponse rpcResponse = post(request);
        if (rpcResponse.isStatus()) {
            JSONObject data = (JSONObject) rpcResponse.getData();
            return data.toJavaObject(method.getReturnType());
        } else {
            Exception ex = rpcResponse.getEx();
//            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
//        return null;
    }
    // 可以改为 Gson
    private RpcResponse post(RpcRequest rpcRequest) {

        try {
            String requestJson = JSON.toJSONString(rpcRequest);
            System.out.println(" ===> reqJson = " + requestJson);
            Request request = new Request.Builder()
                    .url("http://127.0.0.1:8080")
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
