package api;

import java.util.List;

/**
 * Description for this class.
 *
 * @create 2024/3/16 19:12
 */
public interface Router<T> {

    List<T> route(List<T> providers);

    Router Default = p -> p;

}
