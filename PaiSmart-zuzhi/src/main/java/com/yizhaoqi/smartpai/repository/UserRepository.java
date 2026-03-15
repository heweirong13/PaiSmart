package com.yizhaoqi.smartpai.repository;

import com.yizhaoqi.smartpai.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 在 AdminController 中调用 userRepository.findByUsername(username) 时，
// Spring 会自动执行相应的数据库查询操作，返回 Optional<User> 结果。
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
