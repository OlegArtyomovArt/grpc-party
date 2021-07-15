package me.lecoding.grpclearning;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import me.lecoding.grpclearning.common.Constant;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ChatClient {
    private final ManagedChannel channel;
    private ChatRoomGrpc.ChatRoomBlockingStub blockingStub;
    private StreamObserver<Chat.ChatRequest> chat;
    private String token = "";
    private boolean Loggined = false;
    public ChatClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext().build());
    }

    private ChatClient(ManagedChannel channel) {
        this.channel = channel;
        blockingStub = ChatRoomGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public boolean login(String name) {
        Chat.LoginRequest request = Chat.LoginRequest.newBuilder().setName(name).build();
        Chat.LoginResponse response;
        try {
            response = blockingStub.login(request);
        } catch (StatusRuntimeException e) {
            log.error("rpc failed with status: {}, message: {}",e.getStatus(), e.getMessage());
            return false;
        }
        log.info("login with name {} OK!",name);
        this.token = response.getToken();
        this.Loggined = true;
        startReceive();
        return true;
    }

    private void startReceive(){
        Metadata meta = new Metadata();
        meta.put(Constant.HEADER_ROLE,this.token);

        chat =  MetadataUtils.attachHeaders(ChatRoomGrpc.newStub(this.channel),meta).chat(new StreamObserver<Chat.ChatResponse>() {
            @Override
            public void onNext(Chat.ChatResponse value) {
                switch (value.getEventCase()){
                    case ROLE_LOGIN:
                    {
                        log.info("user {}:login!!",value.getRoleLogin().getName());
                    }
                    break;
                    case ROLE_LOGOUT:
                    {
                        log.info("user {}:logout!!",value.getRoleLogout().getName());
                    }
                    break;
                    case ROLE_MESSAGE:
                    {
                        log.info("user {}:{}",value.getRoleMessage().getName(),value.getRoleMessage().getMsg());
                    }
                    break;
                    case EVENT_NOT_SET:
                    {
                        log.error("receive event error:{}",value);
                    }
                    break;
                    case SERVER_SHUTDOWN:
                    {
                        log.info("server closed!");
                        logout();
                    }
                    break;
                }
            }
            @Override
            public void onError(Throwable t) {
                log.error("got error from server:{}",t.getMessage(),t);
            }

            @Override
            public void onCompleted() {
                log.info("closed by server");
            }
        });
        Metadata header = new Metadata();
        header.put(Constant.HEADER_ROLE,this.token);
    }

    public void sendMessage(String msg) throws InterruptedException {
        if("LOGOUT".equals(msg)){
            this.chat.onCompleted();
            this.logout();
            this.Loggined = false;
            shutdown();
        }else{
            if(this.chat != null) this.chat.onNext(Chat.ChatRequest.newBuilder().setMessage(msg).build());
        }
    }

    public void logout(){
        Chat.LogoutResponse resp = blockingStub.logout(Chat.LogoutRequest.newBuilder().build());
        log.info("logout result:{}",resp);
    }

    public static void main(String[] args) throws InterruptedException {
        ChatClient client = new ChatClient("localhost", 8000);
        try {
            String name = "";
            Scanner sc = new Scanner(System.in);
            do{
                System.out.println("please input your nickname");
                name = sc.nextLine();
            }while (!client.login(name));

            while(client.Loggined){
                name = sc.nextLine();
                if(client.Loggined)client.sendMessage(name);
            }
        } finally {
            client.shutdown();
        }
    }
}
