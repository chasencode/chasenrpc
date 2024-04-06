package cn.chasen.rpc.core.cluster;

import cn.chasen.rpc.core.api.LoadBalancer;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @program: chasenrpc
 * @description: 自定义负载均衡
 * @author: Chasen
 * @create: 2024-04-06 22:15
 **/
public class RoundRibonLoadBalancer<T> implements LoadBalancer<T> {

    AtomicInteger index = new AtomicInteger(0);
    @Override
    public T choose(List<T> providers) {
        if(providers == null || providers.isEmpty()) return null;
        if(providers.size() == 1) return providers.get(0);
        return providers.get((index.getAndIncrement()&0x7fffffff) % providers.size());
    }
}
