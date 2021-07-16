package com.party.participant.config;

import com.google.gson.Gson;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Value;

import javax.inject.Singleton;

@Factory
public class CommonConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Singleton
    public Gson gson() {
        return new Gson();
    }

    @Singleton
    public JWSSigner jwtSinger() {
        try {
            return new MACSigner(secret);
        } catch (Exception e) {
            throw new RuntimeException("create signer error", e);
        }
    }

    @Singleton
    public JWSVerifier jwtVerifier() {
        try {
            return new MACVerifier(secret);
        } catch (Exception e) {
            throw new RuntimeException("create signer error", e);
        }
    }
}