package cn.chasen.rpc.core.config;

import cn.chasen.rpc.core.api.*;
import cn.chasen.rpc.core.cluster.GrayRouter;
import cn.chasen.rpc.core.cluster.RoundRibonLoadBalancer;
import cn.chasen.rpc.core.consuemer.ConsumerBootstrap;
import cn.chasen.rpc.core.filter.CacheFilter;
import cn.chasen.rpc.core.filter.ParameterFilter;
import lombok.extern.slf4j.Slf4j;
import cn.chasen.rpc.core.meta.InstanceMeta;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import cn.chasen.rpc.core.registry.zk.ZkRegistryCenter;

import java.util.List;

/**
 * @program: chasenrpc
 * @description: 消费者配置加载
 * @author: Chasen
 * @create: 2024-04-06 22:15
 **/
@Configuration
@Slf4j
@Import({ConsumerConfigProperties.class, AppConfigProperties.class})
public class ConsumerConfig {


    @Autowired
    AppConfigProperties appConfigProperties;

    @Autowired
    ConsumerConfigProperties consumerConfigProperties;


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
        return new GrayRouter(consumerConfigProperties.getGrayRatio());
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



    @Bean
    public RpcContext createContext(@Autowired Router router,
                                    @Autowired LoadBalancer loadBalancer,
                                    @Autowired List<Filter> filters) {
        RpcContext context = new RpcContext();
        context.setRouter(router);
        context.setLoadBalancer(loadBalancer);
        context.setFilters(filters);
        context.getParameters().put("app.id", appConfigProperties.getId());
        context.getParameters().put("app.namespace", appConfigProperties.getNamespace());
        context.getParameters().put("app.env", appConfigProperties.getEnv());
        context.getParameters().put("consumer.retries", String.valueOf(consumerConfigProperties.getRetries()));
        context.getParameters().put("consumer.timeout", String.valueOf(consumerConfigProperties.getTimeout()));
        context.getParameters().put("consumer.faultLimit", String.valueOf(consumerConfigProperties.getFaultLimit()));
        context.getParameters().put("consumer.halfOpenInitialDelay", String.valueOf(consumerConfigProperties.getHalfOpenInitialDelay()));
        context.getParameters().put("consumer.halfOpenDelay", String.valueOf(consumerConfigProperties.getHalfOpenDelay()));
        return context;
    }
}
