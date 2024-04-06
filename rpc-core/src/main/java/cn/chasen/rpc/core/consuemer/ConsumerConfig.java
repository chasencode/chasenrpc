package cn.chasen.rpc.core.consuemer;

import cn.chasen.rpc.core.api.Filter;
import cn.chasen.rpc.core.api.LoadBalancer;
import cn.chasen.rpc.core.api.RegistryCenter;
import cn.chasen.rpc.core.api.Router;
import cn.chasen.rpc.core.cluster.GrayRouter;
import cn.chasen.rpc.core.cluster.RoundRibonLoadBalancer;
import cn.chasen.rpc.core.filter.CacheFilter;
import cn.chasen.rpc.core.filter.ParameterFilter;
import lombok.extern.slf4j.Slf4j;
import cn.chasen.rpc.core.meta.InstanceMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import cn.chasen.rpc.core.registry.zk.ZkRegistryCenter;
/**
 * @program: chasenrpc
 * @description: 消费者配置加载
 * @author: Chasen
 * @create: 2024-04-06 22:15
 **/
@Configuration
@Slf4j
public class ConsumerConfig {

    @Value("${chasenrpc.providers}")
    String servers;

    @Value("${app.grayRatio}")
    private int grayRatio;

    @Bean
    ConsumerBootstrap createConsumerBootstrap() {
        return new ConsumerBootstrap();
    }

    /**
     * ApplicationRunner 所有 上下文初始化完毕后才会执行
     * @param consumerBootstrap
     * @return
     */
    @Bean
    @Order(Integer.MIN_VALUE)
    public ApplicationRunner consumerBootstrapRunner(@Autowired ConsumerBootstrap consumerBootstrap) {
        return x-> {
           log.info("consumerBootstrap starting ...");
            consumerBootstrap.start();
           log.info("consumerBootstrap started ...");
        };
    }

    @Bean
    public LoadBalancer<InstanceMeta> loadBalancer() {
        //return LoadBalancer.Default;
        return new RoundRibonLoadBalancer<>();
    }

    @Bean
    public Router<InstanceMeta> router() {
        return new GrayRouter(grayRatio);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public RegistryCenter consumerRc() {
        return new ZkRegistryCenter();
    }

    /**
     * @todo 改为配置 filetr
     * @return filter
     */
    @Bean
    public Filter filter() {
        return new ParameterFilter();
    }
}
