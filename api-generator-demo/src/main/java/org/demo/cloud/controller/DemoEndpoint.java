package org.demo.cloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.lang.Long;
import lombok.extern.slf4j.Slf4j;
import org.demo.cloud.dto.RegisterRequest;
import org.demo.cloud.dto.UserResponse;
import org.demo.cloud.dto.UserUpdateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@Tag(
        name = "Demo端点"
)
public class DemoEndpoint {
    @PostMapping("/users/instance")
    @Operation(
            summary = "Register a new user"
    )
    public Long postUsersInstance(@RequestBody RegisterRequest request) {
        // todo 实现业务逻辑
        return 1L;
    }

    @PutMapping("/users/instance")
    @Operation(
            summary = "Update a user"
    )
    public UserResponse putUsersInstance(@RequestBody UserUpdateRequest request) {
        // todo 实现业务逻辑
        return new UserResponse();
    }

    @GetMapping("/users/instance")
    @Operation(
            summary = "Get user by ID"
    )
    public UserResponse getUsersInstance(@RequestParam("id") Long id) {
        // todo 实现业务逻辑
        return new UserResponse();
    }
}
