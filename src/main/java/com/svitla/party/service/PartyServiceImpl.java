package com.svitla.party.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Maps;
import com.google.protobuf.util.Timestamps;
import com.svitla.party.PartyGrpc;
import com.svitla.party.PartyOuterClass;
import com.svitla.party.common.Constant;
import com.svitla.party.common.JWTUtils;
import com.svitla.party.interceptor.RoleServerInterceptor;
import com.svitla.party.manager.OnlineUserManager;
import com.svitla.party.user.UserDTO;
import com.svitla.party.user.UserService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


@GRpcService(interceptors = {RoleServerInterceptor.class})
@Slf4j
public class PartyServiceImpl extends PartyGrpc.PartyImplBase {
    @Autowired
    private UserService userService;
    @Autowired
    private OnlineUserManager onlineUserManager;
    @Autowired
    private JWTUtils jwtUtils;

    private Map<String, StreamObserver<PartyOuterClass.HealthResponse>> clients = Maps.newHashMap();

    private Cache<String, Long> health = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .removalListener((RemovalListener<String, Long>) notification -> cleanDataAndNotifyAll(notification.getKey()))
            .build();


    @Override
    public void login(PartyOuterClass.LoginRequest request, io.grpc.stub.StreamObserver<PartyOuterClass.LoginResponse> responseObserver) {
        UserDTO user = userService.checkUser(request.getName(), request.getPassword());
        if (Objects.isNull(user)) {
            responseObserver.onError(Status.fromCode(Status.UNAUTHENTICATED.getCode()).withDescription("uasername or password error").asRuntimeException());
            return;
        }
        loginUser(user);
        responseObserver.onNext(PartyOuterClass.LoginResponse.newBuilder().setToken(jwtUtils.generateToken(user.getUserName())).build());
        responseObserver.onCompleted();
        log.info("user {} login OK!", request.getName());
        //Notify all users that  new user logged to party
        broadcast(PartyOuterClass.HealthResponse
                .newBuilder()
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .setRoleLogin(
                        PartyOuterClass.HealthResponse.Login
                                .newBuilder()
                                .setName(request.getName())
                                .build()
                ).build());
    }

    private void loginUser(UserDTO user) {
        onlineUserManager.addUser(user);
        health.put(user.getUserName(), System.currentTimeMillis());
    }

    @Override
    public void logout(PartyOuterClass.LogoutRequest request,
                       io.grpc.stub.StreamObserver<PartyOuterClass.LogoutResponse> responseObserver) {
        UserDTO user = Constant.CONTEXT_ROLE.get();
        if (!Objects.isNull(user)) {
            log.info("user logout:{}", user.getUserName());
            userLogout(user);
        }
        responseObserver.onNext(PartyOuterClass.LogoutResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public io.grpc.stub.StreamObserver<PartyOuterClass.HealthRequest> health(
            io.grpc.stub.StreamObserver<PartyOuterClass.HealthResponse> responseObserver) {
        UserDTO user = Constant.CONTEXT_ROLE.get();
        if (Objects.isNull(user)) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("need login first").asRuntimeException());
            return null;
        }
        clients.put(user.getUserName(), responseObserver);

        return new StreamObserver<PartyOuterClass.HealthRequest>() {
            @Override
            public void onNext(PartyOuterClass.HealthRequest value) {
                log.info("got health message from {}: {}", user.getUserName(), value.getMessage());
                try {
                    health.get(user.getUserName(), System::currentTimeMillis);
                } catch (Exception e) {
                    log.error("Error dur load", e);
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("got error from {}", user.getUserName(), t);
                userLogout(user);
            }

            @Override
            public void onCompleted() {
                log.info("Completed");
                userLogout(user);
            }
        };
    }

    private void userLogout(UserDTO user) {
        health.invalidate(user.getUserName());
    }

    private void cleanDataAndNotifyAll(String userName) {
        log.info("Logout for user: {}", userName);
        clients.remove(userName);
        onlineUserManager.removeUserById(userName);
        //Notify all users that  new user logged out from party
        broadcast(PartyOuterClass.HealthResponse
                .newBuilder()
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .setRoleLogout(
                        PartyOuterClass.HealthResponse.Logout
                                .newBuilder()
                                .setName(userName)
                                .build()
                ).build());
    }

    private void broadcast(PartyOuterClass.HealthResponse msg) {
        for (StreamObserver<PartyOuterClass.HealthResponse> resp : clients.values()) {
            resp.onNext(msg);
        }
    }

    @Scheduled(cron = "${cleanInActiveUsers.cron}")
    public void clearCacheTask(){
        long currentSize = health.size();
        health.cleanUp();
        long sizeAfterCache = health.size();
        log.info("Cache data before clean: {}, after: {}", currentSize, sizeAfterCache);
    }

}
