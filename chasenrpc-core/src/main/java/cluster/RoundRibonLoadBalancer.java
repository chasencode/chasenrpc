package cluster;

import api.LoadBalancer;
import meta.InstanceMeta;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description for this class.
 *
 * @create 2024/3/16 19:53
 */
public class RoundRibonLoadBalancer<T> implements LoadBalancer<T> {

    AtomicInteger index = new AtomicInteger(0);
    @Override
    public T choose(List<T> providers) {
        if(providers == null || providers.isEmpty()) return null;
        if(providers.size() == 1) return providers.get(0);
        return providers.get((index.getAndIncrement()&0x7fffffff) % providers.size());
    }
}
