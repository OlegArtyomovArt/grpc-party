package com.party.participant.grpc;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.protobuf.util.Timestamps;
import com.party.participant.security.Role;
import com.party.participant.security.JwtService;
import com.party.participant.grpc.generated.HealthRequest;
import com.party.participant.grpc.generated.HealthResponse;
import com.party.participant.grpc.generated.JoinRequest;
import com.party.participant.grpc.generated.JoinResponse;
import com.party.participant.grpc.generated.LeaveRequest;
import com.party.participant.grpc.generated.LeaveResponse;
import com.party.participant.grpc.generated.PartyGrpc;
import com.party.participant.user.OnlineUserManager;
import com.party.participant.user.UserAuthenticationService;
import com.party.participant.user.User;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.micronaut.scheduling.annotation.Scheduled;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class PartyGrpcService extends PartyGrpc.PartyImplBase {

    private final UserAuthenticationService userAuthenticationService;
    private final OnlineUserManager onlineUserManager;
    private final JwtService jwtService;

    private final Map<String, StreamObserver<HealthResponse>> clients = new HashMap<>();
    private final Cache<String, Long> health = CacheBuilder.newBuilder()
            .expireAfterAccess(2, TimeUnit.MINUTES)
            .removalListener((RemovalListener<String, Long>) notification -> cleanDataAndNotifyAll(notification.getKey()))
            .build();

    @Inject
    public PartyGrpcService(UserAuthenticationService userAuthenticationService,
                            OnlineUserManager onlineUserManager,
                            JwtService jwtService) {
        this.userAuthenticationService = userAuthenticationService;
        this.onlineUserManager = onlineUserManager;
        this.jwtService = jwtService;
    }

    @Override
    public void login(JoinRequest request, StreamObserver<JoinResponse> responseObserver) {
        User user = userAuthenticationService.authenticate(request.getName(), request.getPassword());
        if (Objects.isNull(user)) {
            responseObserver.onError(Status.fromCode(Status.UNAUTHENTICATED.getCode()).withDescription("username or password error").asRuntimeException());
            return;
        }
        loginUser(user);
        responseObserver.onNext(JoinResponse.newBuilder().setToken(jwtService.generateToken(user.getUserName())).build());
        responseObserver.onCompleted();
        log.info("user {} login OK!", request.getName());
        //Notify all users that  new user logged to party
        broadcast(HealthResponse
                .newBuilder()
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .setRoleLogin(
                        HealthResponse.Login
                                .newBuilder()
                                .setName(request.getName())
                                .build()
                ).build());
    }

    private void loginUser(User user) {
        onlineUserManager.addUser(user);
        health.put(user.getUserName(), System.currentTimeMillis());
    }

    @Override
    public void logout(LeaveRequest request,
                       StreamObserver<LeaveResponse> responseObserver) {
        User user = Role.CONTEXT_ROLE.get();
        if (!Objects.isNull(user)) {
            log.info("user logout:{}", user.getUserName());
            userLogout(user);
        }
        responseObserver.onNext(LeaveResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public StreamObserver<HealthRequest> health(
            StreamObserver<HealthResponse> responseObserver) {
        User user = Role.CONTEXT_ROLE.get();
        if (Objects.isNull(user)) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("need login first").asRuntimeException());
            return null;
        }
        clients.put(user.getUserName(), responseObserver);

        return new StreamObserver<>() {
            @Override
            public void onNext(HealthRequest value) {
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

    private void userLogout(User user) {
        health.invalidate(user.getUserName());
    }

    private void cleanDataAndNotifyAll(String userName) {
        log.info("Logout for user: {}", userName);
        clients.remove(userName);
        onlineUserManager.removeById(userName);
        //Notify all users that  new user logged out from party
        broadcast(HealthResponse
                .newBuilder()
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .setRoleLogout(
                        HealthResponse.Logout
                                .newBuilder()
                                .setName(userName)
                                .build()
                ).build());
    }

    private void broadcast(HealthResponse msg) {
        for (StreamObserver<HealthResponse> resp : clients.values()) {
            resp.onNext(msg);
        }
    }

    @Scheduled(cron = "${cleanInActiveUsers.cron}")
    public void clearCacheTask() {
        long currentSize = health.size();
        health.cleanUp();
        long sizeAfterCache = health.size();
        log.info("Cache data before clean: {}, after: {}", currentSize, sizeAfterCache);
    }
}