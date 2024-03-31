package cn.chasen.rpc.core.api;

import java.util.List;

/**
 * 负载均衡，weightedRR，AAWR-自适应，
 * 8081， w=100, 25次
 * 8082， w=300, 75次
 *
 * 0-99，random，<25， -8081， else  8082
 *
 * UserService  10，，，
 * 8081， 10ms，
 * 8082， 100ms，
 *
 * avg * 0.3 + last * 0.7 = W* ~
 *
 * @create 2024/3/16 19:12
 */
public interface LoadBalancer<T> {

    T choose(List<T> providers);

    LoadBalancer Default = p -> (p == null || p.size() == 0) ? null : p.get(0);

}
