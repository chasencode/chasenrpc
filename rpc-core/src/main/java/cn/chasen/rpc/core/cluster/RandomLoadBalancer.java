package cn.chasen.rpc.core.cluster;


import cn.chasen.rpc.core.api.LoadBalancer;

import java.util.List;
import java.util.Random;

/**
 * @program: chasenrpc
 * @description: 随机负载均衡
 * @author: Chasen
 * @create: 2024-04-06 22:15
 **/
public class RandomLoadBalancer<T> implements LoadBalancer<T> {

    Random random = new Random();
    @Override
    public T choose(List<T> providers) {
        if(providers == null || providers.isEmpty()) return null;
        if(providers.size() == 1) return providers.get(0);
        return providers.get(random.nextInt(providers.size()));
    }
}
