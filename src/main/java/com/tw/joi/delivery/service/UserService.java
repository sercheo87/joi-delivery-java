package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.seedData.SeedData;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final List<User> users= SeedData.users;

    public User fetchUserById(String userId) {
        return users.stream()
            .filter(user -> userId.equals(user.getUserId()))
            .findFirst()
            .orElse(null);
    }

}
