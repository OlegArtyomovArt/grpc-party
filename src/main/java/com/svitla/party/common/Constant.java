package com.svitla.party.common;

import com.svitla.party.user.UserDTO;
import io.grpc.Context;
import io.grpc.Metadata;

public class Constant {
    public static final Metadata.Key<String> HEADER_ROLE = Metadata.Key.of("role_name",Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<UserDTO> CONTEXT_ROLE = Context.key("role_name");
}
