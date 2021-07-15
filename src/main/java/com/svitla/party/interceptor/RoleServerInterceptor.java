package com.svitla.party.interceptor;

import com.svitla.party.common.Constant;
import com.svitla.party.common.JWTUtils;
import com.svitla.party.user.UserDTO;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import com.svitla.party.service.OnlineUserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RoleServerInterceptor implements ServerInterceptor {

    @Autowired
    private OnlineUserManager onlineUserManager;

    @Autowired
    private JWTUtils jwtUtils;

    private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {
    };

    @SuppressWarnings("unchecked")
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context ctx = Context.current();
        if (!"me.lecoding.grpclearning.Party/Login".equals(call.getMethodDescriptor().getFullMethodName())) {
            String token = headers.get(Constant.HEADER_ROLE);
            if (token == null) {
                call.close(Status.UNAUTHENTICATED.withDescription("need login first!"), headers);
                return NOOP_LISTENER;
            }
            String userId = jwtUtils.checkToken(token);
            UserDTO user = onlineUserManager.findUserById(userId);
            if (user == null) {
                call.close(Status.UNAUTHENTICATED.withDescription("token error!"), headers);
                return NOOP_LISTENER;
            }
            ctx = ctx.withValue(Constant.CONTEXT_ROLE, user);
        }
        return Contexts.interceptCall(ctx, call, headers, next);
    }

}
