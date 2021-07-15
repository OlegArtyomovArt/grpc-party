package com.svitla.party.user;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserDTO {

    private String userName;

    private String password;
}
