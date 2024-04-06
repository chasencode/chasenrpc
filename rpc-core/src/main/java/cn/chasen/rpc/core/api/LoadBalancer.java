package cn.chasen.rpc.core.api;

import java.util.List;

/**
 * @program: chasenrpc
 * @description: 负载均衡接口
 * @author: Chasen
 * @create: 2024-04-06 22:15
 **/
public interface LoadBalancer<T> {

    T choose(List<T> providers);

    LoadBalancer Default = p -> (p == null || p.size() == 0) ? null : p.get(0);

}
