package me.lecoding.grpclearning.user;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserDTO {

    private String userName;

    private String password;
}
