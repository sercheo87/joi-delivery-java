package com.tw.joi.delivery.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tw.joi.delivery.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    private final UserService userService = new UserService();

    @Test
    @DisplayName("Given an existing user id, when the user is requested, then the matching user is returned")
    void shouldFetchUserById() {
        // Given
        String userId = "user101";

        // When
        User user = userService.fetchUserById(userId);

        // Then
        assertThat(user).isNotNull();
        assertThat(user.getUserId()).isEqualTo(userId);
        assertThat(user.getFirstName()).isEqualTo("John");
        assertThat(user.getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("Given an unknown user id, when the user is requested, then no user is returned")
    void shouldReturnNullWhenUserDoesNotExist() {
        // Given
        String userId = "unknown-user";

        // When
        User user = userService.fetchUserById(userId);

        // Then
        assertThat(user).isNull();
    }
}
