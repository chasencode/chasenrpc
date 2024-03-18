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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@Import({ConsumerConfig.class})
@RestController
public class ChasenDemoConsumerApplication {

    @ChasenConsumer
    UserService userService;

    @ChasenConsumer
    OrderService orderService;

    @RequestMapping("/")
    public User findBy(int id) {
        return userService.findById(id);
    }

    @Autowired
    Demo2 demo2;

    public static void main(String[] args) {
        SpringApplication.run(ChasenDemoConsumerApplication.class, args);
    }

    @Bean
    ApplicationRunner providerRun() {
        return x -> {
//            System.out.printf("这里");
//            User user = userService.findById(1);
//            System.out.println("RPC result userService.findById(1) = " + user);
//            User user2 = userService.findById(1, "Chasen");
//            System.out.println("RPC result userService.findById(1) = " + user2);
//            User user3 = userService.findUserOrder(102);
//            System.out.println("RPC result userService.findUserOrder(1) = " + user3);
//            demo2.test();
//            Long id = userService.findLong(102L);
//            System.out.println("RPC result userService.findLong(1) = " + id);
//            Long userId = userService.findLong(new User(1, "Chasen-2", new Order(2L, 1.2f)));
//            System.out.println("RPC result userService.findLong(1) = " + userId);
//
//            Long floatId = userService.getFloatId(111f);
//            System.out.println("RPC result userService.getFloatId(1) = " + floatId);
//            Order order404 = orderService.findById(404);
//            System.out.println("RPC result orderService.findById(2) = " + order404);
//
//            int[] intIds = userService.getIntIds();
//            System.out.println("RPC result userService.getIntIds(1) = " + intIds);
//            long[] getLongIds = userService.getLongIds();
//            System.out.println("RPC result userService.getLongIds(1) = " + getLongIds);
            int[] indIds = userService.getIds(new int[]{1,1,1,1});
            System.out.println("RPC result userService.getIds(1) = " + indIds);
        };
    }
}
