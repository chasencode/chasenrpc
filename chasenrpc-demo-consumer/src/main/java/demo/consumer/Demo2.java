package demo.consumer;


import annotation.ChasenConsumer;
import demo.api.User;
import demo.api.UserService;
import org.springframework.stereotype.Component;

@Component
public class Demo2 {

    @ChasenConsumer
    UserService userService;

    public void test() {
        User user = userService.findById(100);
        System.out.println(user);
    }

}
