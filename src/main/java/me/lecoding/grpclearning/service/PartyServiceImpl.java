package me.lecoding.grpclearning.service;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.collect.Maps;
import com.google.protobuf.util.Timestamps;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import me.lecoding.grpclearning.PartyGrpc;
import me.lecoding.grpclearning.PartyOuterClass;
import me.lecoding.grpclearning.common.Constant;
import me.lecoding.grpclearning.common.JWTUtils;
import me.lecoding.grpclearning.interceptor.RoleServerInterceptor;
import me.lecoding.grpclearning.manager.OnlineUserManager;
import me.lecoding.grpclearning.user.UserDTO;
import me.lecoding.grpclearning.user.UserService;
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
    public void login(me.lecoding.grpclearning.PartyOuterClass.LoginRequest request, io.grpc.stub.StreamObserver<me.lecoding.grpclearning.PartyOuterClass.LoginResponse> responseObserver) {
        UserDTO user = userService.checkUser(request.getName(), request.getPassword());
        if (Objects.isNull(user)) {
            responseObserver.onError(Status.fromCode(Status.UNAUTHENTICATED.getCode()).withDescription("uasername or password error").asRuntimeException());
            return;
        }
        loginUser(user);
        responseObserver.onNext(me.lecoding.grpclearning.PartyOuterClass.LoginResponse.newBuilder().setToken(jwtUtils.generateToken(user.getUserName())).build());
        responseObserver.onCompleted();
        log.info("user {} login OK!", request.getName());
        //Notify all users that  new user logged to party
        broadcast(me.lecoding.grpclearning.PartyOuterClass.HealthResponse
                .newBuilder()
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .setRoleLogin(
                        me.lecoding.grpclearning.PartyOuterClass.HealthResponse.Login
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
    public void logout(me.lecoding.grpclearning.PartyOuterClass.LogoutRequest request,
                       io.grpc.stub.StreamObserver<me.lecoding.grpclearning.PartyOuterClass.LogoutResponse> responseObserver) {
        UserDTO user = Constant.CONTEXT_ROLE.get();
        if (!Objects.isNull(user)) {
            log.info("user logout:{}", user.getUserName());
            userLogout(user);
        }
        responseObserver.onNext(me.lecoding.grpclearning.PartyOuterClass.LogoutResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public io.grpc.stub.StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthRequest> health(
            io.grpc.stub.StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthResponse> responseObserver) {
        UserDTO user = Constant.CONTEXT_ROLE.get();
        if (Objects.isNull(user)) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("need login first").asRuntimeException());
            return null;
        }
        clients.put(user.getUserName(), responseObserver);

        return new StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthRequest>() {
            @Override
            public void onNext(me.lecoding.grpclearning.PartyOuterClass.HealthRequest value) {
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
        broadcast(me.lecoding.grpclearning.PartyOuterClass.HealthResponse
                .newBuilder()
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .setRoleLogout(
                        me.lecoding.grpclearning.PartyOuterClass.HealthResponse.Logout
                                .newBuilder()
                                .setName(userName)
                                .build()
                ).build());
    }

    private void broadcast(me.lecoding.grpclearning.PartyOuterClass.HealthResponse msg) {
        for (StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthResponse> resp : clients.values()) {
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
