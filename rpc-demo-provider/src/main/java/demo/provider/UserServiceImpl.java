package demo.provider;

import annotation.ChasenProvider;
import demo.api.Order;
import demo.api.User;
import demo.api.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ChasenProvider
public class UserServiceImpl implements UserService {

    @Autowired
    Environment environment;

    @Override
    public User findById(int id) {
        return new User(id, "Chasen-"
                + environment.getProperty("server.port")
                + "_" + System.currentTimeMillis(), null);
    }

    @Override
    public User findById(Integer id, String name) {
        return new User(id, name, null);
    }

    @Override
    public User findUserOrder(Integer id) {
        Order order = new Order();
        order.setAmount(1.1f);
        order.setId(1L);
        return new User(id, "'userOrder'", order);
    }

    @Override
    public Long findLong(Long id) {
        return id;
    }

    @Override
    public Long findLong(User user) {
        return user.getOrder().getId();
    }

    @Override
    public long getFloatId(float id) {
        return 2L;
    }

    @Override
    public int[] getIntIds() {
        return new int[]{1,2,3};
    }

    @Override
    public long[] getLongIds() {
        return new long[]{4L,5L,6L};
    }

    @Override
    public int[] getIds(int[] ids) {
        return ids;
    }
}
