package consuemer;

import api.LoadBalancer;
import api.RegistryCenter;
import api.Router;
import cluster.RoundRibonLoadBalancer;
import lombok.extern.slf4j.Slf4j;
import meta.InstanceMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import registry.zk.ZkRegistryCenter;

@Configuration
@Slf4j
public class ConsumerConfig {

    @Value("${chasenrpc.providers}")
    String servers;

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
        return Router.Default;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public RegistryCenter consumerRc() {
        return new ZkRegistryCenter();
    }
}
