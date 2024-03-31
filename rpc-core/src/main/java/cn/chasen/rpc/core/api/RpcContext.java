package cn.chasen.rpc.core.api;

import cn.chasen.rpc.core.meta.InstanceMeta;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class RpcContext {

    private Router<InstanceMeta> router;

    private LoadBalancer<InstanceMeta> loadBalancer;

    private List<Filter> filters;

    private Map<String, String> parameters = new HashMap<>();


}
