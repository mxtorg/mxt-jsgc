package org.demo.cloud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.lang.String;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(
        description = "用户更新请求体"
)
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    @NotBlank(
            message = "username不能为空"
    )
    @Size(
            min = 3,
            message = "username长度不能少于3位"
    )
    @Schema(
            description = "username"
    )
    private String username;

    @NotBlank(
            message = "email不能为空"
    )
    @Email(
            message = "email格式不正确"
    )
    @Schema(
            description = "email"
    )
    private String email;

    @NotBlank(
            message = "password不能为空"
    )
    @Size(
            min = 8,
            message = "password长度不能少于8位"
    )
    @Schema(
            description = "password"
    )
    private String password;
}
