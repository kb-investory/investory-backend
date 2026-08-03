package com.investory.auth.infra.clients.naver;

import lombok.Data;

@Data
public class NaverUserResponse {

    private String resultcode;
    private String message;
    private Response response;

    @Data
    public static class Response {
        private String id;
        private String email;
        private String nickname;
        private String name; // 네이버가 nickname을 안 내려줄 때(동의 안 함/미설정) 대체용 실명
    }
}