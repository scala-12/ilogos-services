package com.ilogos.user.user;

import java.util.Optional;
import java.util.UUID;

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
import user.User.UserIdRequest;
import user.User.UserInfoResponse;
import user.UserServiceGrpc;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;

    private interface IUserInfo extends IUserBase, IWithRoles, IWithPassword {
    }

    private static void sendUserInfo(StreamObserver<UserInfoResponse> responseObserver,
            Optional<IUserInfo> userInfo) {
        userInfo.ifPresentOrElse(user -> {
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

    @Override
    public void findUserByEmailOrUsername(UserEmailOrUsernameRequest request,
            StreamObserver<UserInfoResponse> responseObserver) {
        var usernameOrEmail = request.getUsernameOrEmail();
        var userInfo = userRepository.<IUserInfo>findByEmailOrUsername(usernameOrEmail, usernameOrEmail,
                IUserInfo.class);
        sendUserInfo(responseObserver, userInfo);
    }

    @Override
    public void findUserById(UserIdRequest request,
            StreamObserver<UserInfoResponse> responseObserver) {
        var userId = request.getId();
        var userInfo = userRepository.<IUserInfo>findUserById(UUID.fromString(userId), IUserInfo.class);
        sendUserInfo(responseObserver, userInfo);
    }

}
