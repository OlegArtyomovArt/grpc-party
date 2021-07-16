package com.party.participant.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Date;

@Slf4j
@Singleton
public class JwtService {

    private static final long DEFAULT_EXPIRE_TIME = 30 * 60 * 1000;

    private final JWSSigner jwtSinger;
    private final JWSVerifier jwtVerifier;

    @Inject
    public JwtService(JWSSigner jwtSinger,
                      JWSVerifier jwtVerifier) {
        this.jwtSinger = jwtSinger;
        this.jwtVerifier = jwtVerifier;
    }

    public String generateToken(String username) {
        JWTClaimsSet claimSet = new JWTClaimsSet.Builder()
                .subject(username)
                .expirationTime(new Date(new Date().getTime() + DEFAULT_EXPIRE_TIME))
                .build();
        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimSet);
        try {
            signedJWT.sign(jwtSinger);
        } catch (JOSEException e) {
            log.error("Error during sign", e);
            throw new RuntimeException(e);
        }
        return signedJWT.serialize();
    }

    public String checkToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            if (!signedJWT.verify(jwtVerifier)){
                return null;
            }

            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            log.error("Error during check token", e);
            return null;
        }
    }

}