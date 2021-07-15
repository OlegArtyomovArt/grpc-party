package me.lecoding.grpclearning.service;

import com.google.common.collect.Sets;
import com.google.protobuf.util.Timestamps;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import me.lecoding.grpclearning.PartyGrpc;
import me.lecoding.grpclearning.common.Constant;
import me.lecoding.grpclearning.common.JWTUtils;
import me.lecoding.grpclearning.interceptor.RoleServerInterceptor;
import me.lecoding.grpclearning.manager.OnlineUserManager;
import me.lecoding.grpclearning.user.UserDTO;
import me.lecoding.grpclearning.user.UserService;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;
import java.util.Set;

@GRpcService(interceptors = {RoleServerInterceptor.class})
@Slf4j
public class ChatRoomServiceImpl extends PartyGrpc.PartyImplBase {
    @Autowired
    private UserService userService;
    @Autowired
    private OnlineUserManager onlineUserManager;
    @Autowired
    private JWTUtils jwtUtils;

    private Set<StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthResponse>> clients = Sets.newConcurrentHashSet();

    @Override
    public void login(me.lecoding.grpclearning.PartyOuterClass.LoginRequest  request, io.grpc.stub.StreamObserver<me.lecoding.grpclearning.PartyOuterClass.LoginResponse> responseObserver) {
        UserDTO user= userService.checkUser(request.getName(),request.getPassword());
        if(Objects.isNull(user)){
            responseObserver.onError(Status.fromCode(Status.UNAUTHENTICATED.getCode()).withDescription("uasername or password error").asRuntimeException());
            return;
        }
        onlineUserManager.addUser(user);
        responseObserver.onNext(me.lecoding.grpclearning.PartyOuterClass.LoginResponse.newBuilder().setToken(jwtUtils.generateToken(user.getUserName())).build());
        responseObserver.onCompleted();
        log.info("user {} login OK!",request.getName());
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

    @Override
    public void logout(me.lecoding.grpclearning.PartyOuterClass.LogoutRequest request,
                       io.grpc.stub.StreamObserver<me.lecoding.grpclearning.PartyOuterClass.LogoutResponse> responseObserver) {
        UserDTO user = Constant.CONTEXT_ROLE.get();
        if(!Objects.isNull(user)) {
            log.info("user logout:{}", user.getUserName());
            onlineUserManager.removeUserById(user.getUserName());
        }
        responseObserver.onNext(me.lecoding.grpclearning.PartyOuterClass.LogoutResponse.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public io.grpc.stub.StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthRequest> health(
            io.grpc.stub.StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthResponse> responseObserver) {
        clients.add(responseObserver);
        UserDTO user = Constant.CONTEXT_ROLE.get();
        if(Objects.isNull(user)) {
            responseObserver.onError(Status.UNAUTHENTICATED.withDescription("need login first").asRuntimeException());
            return null;
        }


        return new StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthRequest>() {
            @Override
            public void onNext(me.lecoding.grpclearning.PartyOuterClass.HealthRequest value) {
                log.info("got message from {} :{}",user.getUserName(),value.getMessage());
                broadcast(me.lecoding.grpclearning.PartyOuterClass.HealthResponse
                        .newBuilder()
                        .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                        .setRoleMessage(
                                me.lecoding.grpclearning.PartyOuterClass.HealthResponse.Message
                                        .newBuilder()
                                        .setMsg(value.getMessage())
                                        .setName(user.getUserName())
                                        .build()
                        ).build());
            }

            @Override
            public void onError(Throwable t) {
                log.error("got error from {}",user.getUserName(),t);
                userLogout(responseObserver,user);
            }
            @Override
            public void onCompleted() {
                userLogout(responseObserver,user);
            }
        };
    }

    private void userLogout(StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthResponse> responseObserver,
                            UserDTO user){
        clients.remove(responseObserver);
        broadcast(me.lecoding.grpclearning.PartyOuterClass.HealthResponse
                .newBuilder()
                .setTimestamp(Timestamps.fromMillis(System.currentTimeMillis()))
                .setRoleLogout(
                        me.lecoding.grpclearning.PartyOuterClass.HealthResponse.Logout
                                .newBuilder()
                                .setName(user.getUserName())
                                .build()
                ).build());
    }
    private void broadcast(me.lecoding.grpclearning.PartyOuterClass.HealthResponse msg){
        for(StreamObserver<me.lecoding.grpclearning.PartyOuterClass.HealthResponse> resp : clients){
            resp.onNext(msg);
        }
    }
}
