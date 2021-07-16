package com.party.participant.user;

import com.google.common.collect.ImmutableList;

import javax.inject.Singleton;
import java.util.List;

@Singleton
public class UserAuthenticationService {
    private static final List<User> PREDEFINED_USERS = ImmutableList.<User>builder()
            .add(User.builder()
                    .userName("jack")
                    .password("AAA")
                    .build())
            .add(User.builder()
                    .userName("max")
                    .password("AAA")
                    .build())
            .add(User.builder()
                    .userName("andy")
                    .password("AAA")
                    .build())
            .build();

    public User authenticate(String username, String password) {
        return PREDEFINED_USERS.stream()
                .filter(item -> item.getUserName().equals(username))
                .findFirst()
                .filter(user -> user.getPassword().equals(password))
                .orElse(null);
    }
}