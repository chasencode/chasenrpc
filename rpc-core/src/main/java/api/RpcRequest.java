package api;

import lombok.Data;

@Data
public class RpcRequest {

    private String service; // 接口

    private String methodSign; // 方法签名

    private Object[] args; // 参数 100
}
