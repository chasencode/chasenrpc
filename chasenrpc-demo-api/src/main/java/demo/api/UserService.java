package demo.api;

public interface UserService {

    User findById(int id);

    User findById(Integer id, String name);
}
