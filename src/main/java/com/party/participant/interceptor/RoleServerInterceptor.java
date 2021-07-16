package com.party.participant.interceptor;

import com.party.participant.security.JwtService;
import com.party.participant.security.Role;
import com.party.participant.user.OnlineUserManager;
import com.party.participant.user.User;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class RoleServerInterceptor implements ServerInterceptor {

    private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {
    };

    private final OnlineUserManager onlineUserManager;
    private final JwtService jwtService;

    @Inject
    public RoleServerInterceptor(OnlineUserManager onlineUserManager,
                                 JwtService jwtService) {
        this.onlineUserManager = onlineUserManager;
        this.jwtService = jwtService;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call,
                                                                 Metadata headers,
                                                                 ServerCallHandler<ReqT, RespT> next) {
        Context ctx = Context.current();
        if (!"com.party.participant.Party/Login".equals(call.getMethodDescriptor().getFullMethodName())) {
            String token = headers.get(Role.HEADER_ROLE);
            if (token == null) {
                call.close(Status.UNAUTHENTICATED.withDescription("need login first!"), headers);
                return NOOP_LISTENER;
            }
            String userId = jwtService.checkToken(token);
            User user = onlineUserManager.findById(userId);
            if (user == null) {
                call.close(Status.UNAUTHENTICATED.withDescription("token error!"), headers);
                return NOOP_LISTENER;
            }
            ctx = ctx.withValue(Role.CONTEXT_ROLE, user);
        }
        return Contexts.interceptCall(ctx, call, headers, next);
    }

}
