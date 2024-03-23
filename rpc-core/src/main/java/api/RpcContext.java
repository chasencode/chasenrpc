package api;

import api.LoadBalancer;
import api.Router;
import lombok.Data;
import meta.InstanceMeta;

import java.util.List;

@Data
public class RpcContext {

    private Router<InstanceMeta> router;

    private LoadBalancer<InstanceMeta> loadBalancer;

    private List<Filter> filters;

}
