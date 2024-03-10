package demo.consumer;

import annotation.ChasenConsumer;
import consuemer.ConsumerConfig;
import demo.api.Order;
import demo.api.OrderService;
import demo.api.User;
import demo.api.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Import({ConsumerConfig.class})
public class ChasenDemoConsumerApplication {

    @ChasenConsumer
    UserService userService;

    @ChasenConsumer
    OrderService orderService;

    @Autowired
    Demo2 demo2;

    public static void main(String[] args) {
        SpringApplication.run(ChasenDemoConsumerApplication.class, args);
    }

    @Bean
    ApplicationRunner providerRun() {
        return x -> {
            System.out.printf("这里");
            User user = userService.findById(1);
            System.out.println("RPC result userService.findById(1) = " + user);
            demo2.test();
//            Order order404 = orderService.findById(404);
//            System.out.println("RPC result orderService.findById(2) = " + order404);
        };
    }
}
