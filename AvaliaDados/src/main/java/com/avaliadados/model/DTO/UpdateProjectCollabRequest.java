package com.avaliadados.model.DTO;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProjectCollabRequest {
    private String role;
    private Long durationSeconds;
    private Integer quantity;
    private Long pausaMensalSeconds;
}