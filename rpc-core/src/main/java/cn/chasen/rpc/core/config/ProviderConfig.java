package cn.chasen.rpc.core.config;

import cn.chasen.rpc.core.api.RegistryCenter;
import cn.chasen.rpc.core.provider.ProviderBootstrap;
import cn.chasen.rpc.core.provider.ProviderInvoker;
import cn.chasen.rpc.core.registry.zk.ZkRegistryCenter;
import cn.chasen.rpc.core.transport.SpringBootTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;

/**
 * provider config class.
 *
 * @Author : Chasen
 * @create 2024/3/6 21:33
 */

@Slf4j
@Configuration
@Import({AppConfigProperties.class, ProviderProperties.class, SpringBootTransport.class})
public class ProviderConfig {

    @Value("${server.port:8080}")
    private String port;

    @Autowired
    AppConfigProperties appConfigProperties;

    @Autowired
    ProviderProperties providerProperties;

    @Bean
    ProviderBootstrap providerBootstrap() {
        return new ProviderBootstrap(port, appConfigProperties, providerProperties);
    }

    @Bean
    ProviderInvoker providerInvoker(@Autowired ProviderBootstrap providerBootstrap) {
        return new ProviderInvoker(providerBootstrap);
    }

    @Bean
    @Order(Integer.MIN_VALUE)
    public ApplicationRunner providerBootstrap_runner(@Autowired ProviderBootstrap providerBootstrap) {
        return x -> {
            log.info("providerBootstrap starting ...");
            providerBootstrap.start();
            log.info("providerBootstrap started ...");
        };
    }

    @Bean //(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public RegistryCenter provider_rc() {
        return new ZkRegistryCenter();
    }

}
