package cn.chasen.rpc.core.api;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class RpcRequest {

    private String service; // 接口

    private String methodSign; // 方法签名

    private Object[] args; // 参数 100

    private Map<String, String> params = new HashMap<>();

}
