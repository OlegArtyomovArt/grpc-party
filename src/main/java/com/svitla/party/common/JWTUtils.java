package com.svitla.party.common;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class JWTUtils {
    private static final long DEFAULT_EXPIRE_TIME = 30 * 60 * 1000;

    @Autowired
    private JWSSigner jwtSinger;
    @Autowired
    private JWSVerifier jwtVerifier;

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
            if (!signedJWT.verify(jwtVerifier)) return null;
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            log.error("Error during check token", e);
            return null;
        }
    }

}