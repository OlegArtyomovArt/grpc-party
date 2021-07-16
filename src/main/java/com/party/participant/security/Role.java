package com.party.participant.security;

import com.party.participant.user.User;
import io.grpc.Context;
import io.grpc.Metadata;

public class Role {
    public static final Metadata.Key<String> HEADER_ROLE = Metadata.Key.of("role_name",Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<User> CONTEXT_ROLE = Context.key("role_name");
}
