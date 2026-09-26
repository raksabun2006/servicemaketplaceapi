package com.kh.serviceplatform.features.team.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateTeamRequest(

        @NotNull(message = "logo is required")
        String logo,
        @NotNull(message = "Input your team description to be approve")
        String description,
        @NotBlank(message = "The phone number can be blank")
        String phone ,
        @NotNull(message = "inout their address")
        String address

) {
}
