package demo.api;

import lombok.Data;

@Data
public class RpcRequest {

    private String service; // 接口

    private String method; // 方法 findById

    private Object[] args; // 参数 100
}
