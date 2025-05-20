package com.ilogos.user.user;

import org.springframework.grpc.server.service.GrpcService;

import com.ilogos.user.common.model.IWithPassword;
import com.ilogos.user.common.model.IWithRoles;
import com.ilogos.user.user.model.IUserBase;
import com.ilogos.user.user.model.UserDTO;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import user.User.UserEmailOrUsernameRequest;
import user.User.UserInfoResponse;
import user.UserServiceGrpc;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    private interface IUserInfo extends IUserBase, IWithRoles, IWithPassword {
    }

    @Override
    public void findUserByEmailOrUsername(UserEmailOrUsernameRequest request,
            StreamObserver<UserInfoResponse> responseObserver) {
        var usernameOrEmail = request.getUsernameOrEmail();
        userRepository.<IUserInfo>findByEmailOrUsername(usernameOrEmail, usernameOrEmail, IUserInfo.class)
                .ifPresentOrElse(user -> {
                    UserDTO dto = UserDTO.from(user);
                    UserInfoResponse response = UserInfoResponse.newBuilder()
                            .setId(dto.getId().toString())
                            .setEmail(dto.getEmail())
                            .addAllRoles(dto.getRoleNames())
                            .setUsername(dto.getUsername())
                            .setPassword(user.getPassword())
                            .build();

                    responseObserver.onNext(response);
                    responseObserver.onCompleted();
                }, () -> {
                    responseObserver.onError(Status.NOT_FOUND.asRuntimeException());
                });
    }

}
