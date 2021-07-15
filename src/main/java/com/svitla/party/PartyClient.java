package com.svitla.party;

import com.svitla.party.common.Constant;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PartyClient {
    private final ManagedChannel channel;
    private PartyGrpc.PartyBlockingStub blockingStub;
    private StreamObserver<PartyOuterClass.HealthRequest> chat;
    private String token = "";
    private boolean Loggined = false;

    public PartyClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }

    private PartyClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = PartyGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public boolean login(String name) {
        PartyOuterClass.LoginRequest request = PartyOuterClass.LoginRequest.newBuilder()
                .setName(name)
                .setPassword("AAA")
                .build();
        PartyOuterClass.LoginResponse response;
        try {
            response = blockingStub.login(request);
        } catch (StatusRuntimeException e) {
            log.error("rpc failed with status: {}, message: {}", e.getStatus(), e.getMessage());
            return false;
        }
        log.info("login with name {} OK!", name);
        this.token = response.getToken();
        this.Loggined = true;
        startReceive();
        return true;
    }

    private void startReceive() {
        Metadata meta = new Metadata();
        meta.put(Constant.HEADER_ROLE, this.token);

        chat = MetadataUtils.attachHeaders(PartyGrpc.newStub(this.channel), meta).health(new StreamObserver<PartyOuterClass.HealthResponse>() {
            @Override
            public void onNext(PartyOuterClass.HealthResponse value) {
                switch (value.getEventCase()) {
                    case ROLE_LOGIN: {
                        log.info("user {}:login!!", value.getRoleLogin().getName());
                    }
                    break;
                    case ROLE_LOGOUT: {
                        log.info("user {}:logout!!", value.getRoleLogout().getName());
                    }
                    break;
                    case ROLE_MESSAGE: {
                        log.info("user {}:{}", value.getRoleMessage().getName(), value.getRoleMessage().getMsg());
                    }
                    break;
                    case EVENT_NOT_SET: {
                        log.error("receive event error:{}", value);
                    }
                    break;
                    case SERVER_SHUTDOWN: {
                        log.info("server closed!");
                        logout();
                    }
                    break;
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("got error from server:{}", t.getMessage(), t);
            }

            @Override
            public void onCompleted() {
                log.info("closed by server");
            }
        });
        Metadata header = new Metadata();
        header.put(Constant.HEADER_ROLE, this.token);
    }

    public void sendMessage(String msg) throws InterruptedException {
        if ("LOGOUT".equals(msg)) {
            this.chat.onCompleted();
            this.logout();
            this.Loggined = false;
            shutdown();
        } else {
            if (this.chat != null) this.chat.onNext(PartyOuterClass.HealthRequest.newBuilder().setMessage(msg).build());
        }
    }

    public void logout() {
        PartyOuterClass.LogoutResponse resp = blockingStub.logout(PartyOuterClass.LogoutRequest.newBuilder().build());
        log.info("logout result:{}", resp);
    }

    public static void main(String[] args) throws InterruptedException {
        PartyClient client = new PartyClient("localhost", 8000);
        try {
            String name = "";
            Scanner sc = new Scanner(System.in);
            do {
                System.out.println("please input your nickname");
                name = sc.nextLine();
            } while (!client.login(name));

            while (client.Loggined) {
                name = sc.nextLine();
                if (client.Loggined) client.sendMessage(name);
            }
        } finally {
            client.shutdown();
        }
    }
}
