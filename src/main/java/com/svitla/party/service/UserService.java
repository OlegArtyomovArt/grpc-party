package com.svitla.party.service;

import com.google.common.collect.Lists;
import com.svitla.party.user.UserDTO;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;

/**
 * UserService
 */
@Service
public class UserService {
    private List<UserDTO> predefinedUsers = Lists.newArrayList();

    public UserDTO checkUser(String username, String password) {
        Optional<UserDTO> userDTO = predefinedUsers.stream().filter(item -> item.getUserName().equals(username)).findFirst();
        if (userDTO.isPresent()) {
            UserDTO result = userDTO.get();
            if (result.getPassword().equals(password)) {
                return result;
            }
        }
        return null;
    }


    @PostConstruct
    private void afterConstruction() {
        predefinedUsers.add(UserDTO.builder().userName("jack")
                .password("AAA")
                .build());
        predefinedUsers.add(UserDTO.builder().userName("max")
                .password("AAA")
                .build());
        predefinedUsers.add(UserDTO.builder().userName("andy")
                .password("AAA")
                .build());

    }
}
