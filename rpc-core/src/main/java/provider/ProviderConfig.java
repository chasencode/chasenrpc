package provider;

import api.RegistryCenter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import registry.zk.ZkRegistryCenter;

@Configuration
@Slf4j
public class ProviderConfig {

    @Bean
    ProviderBootstrap init() {
        return new ProviderBootstrap();
    }

    @Bean
    ProviderInvoker providerInvoker(@Autowired ProviderBootstrap providerBootstrap) {
        return new ProviderInvoker(providerBootstrap);
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public RegistryCenter providerRc() {
        return new ZkRegistryCenter();
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
}
