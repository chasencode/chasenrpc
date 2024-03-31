package cn.chasen.rpc.demo.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Description for this class.
 *
 * @Author : Chasen
 * @create 2024/3/6 20:36
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    Integer id;
    String name;

}
