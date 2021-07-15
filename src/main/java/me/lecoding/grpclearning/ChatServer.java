package me.lecoding.grpclearning;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import lombok.extern.slf4j.Slf4j;
import me.lecoding.grpclearning.interceptor.RoleServerInterceptor;
import me.lecoding.grpclearning.service.ChatRoomServiceImpl;

import java.io.IOException;

@Slf4j
public class ChatServer {
    private Server server;
    private void start() throws IOException {
        int port = 8000;
        server = ServerBuilder.forPort(port)
                .addService(ServerInterceptors.intercept(new ChatRoomServiceImpl(),new RoleServerInterceptor()))
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            log.info("*** shutting down grpc server since JVM is shutting down");
            ChatServer.this.stop();
            log.info("*** server shut down");
        }));
    }
    private void stop(){
        if(server != null){
            server.shutdown();
        }
    }
    private void blockUntilShutdown() throws  InterruptedException{
        if(server != null){
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        final ChatServer server = new ChatServer();
        server.start();
        log.info("Server is started");
        server.blockUntilShutdown();
    }
}
