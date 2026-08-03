package com.investory.auth.infra.clients.google;

import lombok.Data;

@Data
public class GoogleUserResponse {
    private String sub;
    private String name;
    private String email;
    private String state;
    private Boolean email_verified;
}