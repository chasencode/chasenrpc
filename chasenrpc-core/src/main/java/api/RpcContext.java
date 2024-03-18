package api;

import lombok.Data;

@Data
public class RpcContext {

    private Router router;

    private LoadBalancer loadBalancer;

}
