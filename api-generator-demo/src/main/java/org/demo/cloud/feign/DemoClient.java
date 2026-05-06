package org.demo.cloud.feign;

import java.lang.Long;
import org.demo.cloud.dto.RegisterRequest;
import org.demo.cloud.dto.UserResponse;
import org.demo.cloud.dto.UserUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "Demo",
        url = "${api.Demo.url:}"
)
public interface DemoClient {
    @PostMapping("/users/instance")
    Long postUsersInstance(@RequestBody RegisterRequest requestBody);

    @PutMapping("/users/instance")
    UserResponse putUsersInstance(@RequestBody UserUpdateRequest requestBody);

    @GetMapping("/users/instance")
    UserResponse getUsersInstance(@RequestParam("id") Long id);
}
