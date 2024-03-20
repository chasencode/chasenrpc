package consuemer.http;

import com.alibaba.fastjson.JSON;
import demo.api.RpcRequest;
import demo.api.RpcResponse;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OkHttpInvoker implements HttpInvoker {
    final static MediaType JSONTYPE = MediaType.get("application/json; charset=utf-8");

    OkHttpClient client;

    public OkHttpInvoker() {

        client = new OkHttpClient().newBuilder()
                .connectionPool(new ConnectionPool(16, 60, TimeUnit.SECONDS))
                .readTimeout(1, TimeUnit.SECONDS)
                .writeTimeout(1, TimeUnit.SECONDS)
                .connectTimeout(1, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public RpcResponse<?> post(RpcRequest rpcRequest, String url) {
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
}
