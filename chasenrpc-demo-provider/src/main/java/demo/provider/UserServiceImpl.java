package demo.provider;

import annotation.ChasenProvider;
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
}
