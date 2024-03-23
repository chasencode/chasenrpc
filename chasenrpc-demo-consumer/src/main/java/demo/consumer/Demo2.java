package demo.consumer;


import annotation.ChasenConsumer;
import demo.api.User;
import demo.api.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Demo2 {

    @ChasenConsumer
    UserService userService;

    public void test() {
        User user = userService.findById(100);
        log.debug(user.toString());
    }

}
