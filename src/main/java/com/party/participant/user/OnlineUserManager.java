package com.party.participant.user;

import com.google.common.collect.Maps;
import com.party.participant.user.User;

import javax.inject.Singleton;
import java.util.Map;

@Singleton
public class OnlineUserManager {

    private final Map<String, User> onlineUsers = Maps.newHashMap();

    public void addUser(User user) {
        onlineUsers.put(user.getUserName(), user);
    }

    public void removeById(String userId) {
        onlineUsers.remove(userId);
    }

    public User findById(String userId) {
        return onlineUsers.get(userId);
    }
}
