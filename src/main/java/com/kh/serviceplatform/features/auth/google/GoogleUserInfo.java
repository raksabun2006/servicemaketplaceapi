package com.kh.serviceplatform.features.auth.google;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfo {
    private String subject;
    private String email;
    private boolean emailVerified;
    private String name;
    private String picture;
    private String givenName;
    private String familyName;
}
