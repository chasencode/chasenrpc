package demo.provider;

import annotation.ChasenProvider;
import demo.api.Order;
import demo.api.User;
import demo.api.UserService;
import org.springframework.stereotype.Component;

@Component
@ChasenProvider
public class UserServiceImpl implements UserService {
    @Override
    public User findById(int id) {
        return new User(id, "Chasen-" + System.currentTimeMillis());
    }

    @Override
    public User findById(Integer id, String name) {
        return new User(id, name);
    }
}
