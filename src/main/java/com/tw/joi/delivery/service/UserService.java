package com.tw.joi.delivery.service;

import com.tw.joi.delivery.domain.User;
import com.tw.joi.delivery.seedData.SeedData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class UserService {

    private final List<User> users = SeedData.users;

    public User fetchUserById(String userId) {
        User user = users.stream()
            .filter(u -> userId.equals(u.getUserId()))
            .findFirst()
            .orElse(null);
        if (user == null) {
            log.warn("User not found: userId={}", userId);
        }
        return user;
    }
}
