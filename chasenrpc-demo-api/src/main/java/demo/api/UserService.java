package demo.api;

public interface UserService {

    User findById(int id);

    User findById(Integer id, String name);

    User findUserOrder(Integer id);

    Long findLong(Long id);

    Long findLong(User user);

    long getFloatId(float id);

    int[] getIntIds();

    long[] getLongIds();

    int[] getIds(int[] ids);
}
