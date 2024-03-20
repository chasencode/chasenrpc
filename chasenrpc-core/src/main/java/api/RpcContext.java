package api;

import lombok.Data;
import meta.InstanceMeta;

@Data
public class RpcContext {

    private Router<InstanceMeta> router;

    private LoadBalancer<InstanceMeta> loadBalancer;

}
