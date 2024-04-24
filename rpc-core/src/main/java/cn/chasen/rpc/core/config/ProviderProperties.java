package cn.chasen.rpc.core.config;

import lombok.Data;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * config provider properties.
 *
 * @Author : Chasen
 * @create 2024/4/3 08:16
 */

@Data
@ConfigurationProperties(prefix = "chasenrpc.provider")
public class ProviderProperties {

    // for provider

    Map<String, String> metas = new HashMap<>();

    @Setter
    String test;

}
