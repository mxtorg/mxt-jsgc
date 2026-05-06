package org.demo.cloud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.Long;
import java.lang.String;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Schema(
        description = "用户响应体"
)
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    @Schema(
            description = "id"
    )
    private Long id;

    @Schema(
            description = "message"
    )
    private String message;
}
