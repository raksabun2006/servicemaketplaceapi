package com.kh.serviceplatform.features.team.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record TeamResponse (
        UUID id ,
        String logo ,
        String description,
        String phone ,
        String address,
        LocalDateTime createAt ,
        LocalDateTime updateAt
){
}
