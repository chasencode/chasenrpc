package cn.chasen.rpc.core.api;

import cn.chasen.rpc.core.config.ConsumerProperties;
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

    public String param(String key) {
        return parameters.get(key);
    }

    public static ThreadLocal<Map<String,String>> ContextParameters = new ThreadLocal<>() {
        @Override
        protected Map<String, String> initialValue() {
            return new HashMap<>();
        }
    };

    private ConsumerProperties consumerProperties;


    public static void setContextParameter(String key, String value) {
        ContextParameters.get().put(key, value);
    }

    public static String getContextParameter(String key) {
        return ContextParameters.get().get(key);
    }

    public static void removeContextParameter(String key) {
        ContextParameters.get().remove(key);
    }

    public ConsumerProperties getConsumerProperties() {
        return consumerProperties;
    }

    public void setConsumerProperties(ConsumerProperties consumerProperties) {
        this.consumerProperties = consumerProperties;
    }
}
