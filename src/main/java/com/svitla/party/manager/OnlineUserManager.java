package com.svitla.party.manager;

import com.google.common.collect.Maps;
import com.svitla.party.user.UserDTO;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class OnlineUserManager {
    private Map<String, UserDTO> onlineUsers = Maps.newHashMap();

    public void addUser(UserDTO user){
        onlineUsers.put(user.getUserName(),user);
    }
    public void removeUserById(String userId){
        onlineUsers.remove(userId);
    }
    public UserDTO findUserById(String userId){
        return onlineUsers.get(userId);
    }
}
