package com.party.participant.user;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class User {

    private final String userName;
    private final String password;
}
