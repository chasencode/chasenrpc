package provider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderConfig {

    @Bean
    ProviderBootstrap init() {
        return new ProviderBootstrap();
    }
}
