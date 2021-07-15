package com.svitla.party.service;

import com.google.common.collect.ImmutableList;
import com.svitla.party.user.UserDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * UserService
 */
@Service
public class UserService {
    private static final List<UserDTO> PREDEFINED_USERS = ImmutableList.<UserDTO>builder()
            .add(UserDTO.builder().userName("jack")
                    .password("AAA")
                    .build())
            .add(UserDTO.builder().userName("max")
                    .password("AAA")
                    .build())
            .add(UserDTO.builder().userName("andy")
                    .password("AAA")
                    .build())
            .build();

    public UserDTO checkUser(String username, String password) {
        Optional<UserDTO> userDTO = PREDEFINED_USERS.stream().filter(item -> item.getUserName().equals(username)).findFirst();
        if (userDTO.isPresent()) {
            UserDTO result = userDTO.get();
            if (result.getPassword().equals(password)) {
                return result;
            }
        }
        return null;
    }


}
