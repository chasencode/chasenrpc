package demo.provider;

import demo.api.RpcRequest;
import demo.api.RpcResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import provider.ProviderBootstrap;
import provider.ProviderConfig;

@SpringBootApplication
@RestController
@Import({ProviderConfig.class})
public class ChasenDemoProviderApplication {

    @Autowired
    ProviderBootstrap providerBootstrap;

    public static void main(String[] args) {
        SpringApplication.run(ChasenDemoProviderApplication.class, args);
    }
    // 使用 HTTP + JSON 来时间序列化和通信


    @RequestMapping("/")
    public RpcResponse invoke(@RequestBody RpcRequest request) {
        return  providerBootstrap.invokeRequest(request);
    }

    @Bean
    ApplicationRunner providerRun() {
        return x -> {
            RpcRequest request = new RpcRequest();
            request.setService("demo.api.UserService");
            request.setMethod("findById");
            request.setArgs(new Object[]{100});
            RpcResponse rpcResponse = providerBootstrap.invokeRequest(request);
            System.out.printf("return:" + rpcResponse.getData());
        };
    }
}
