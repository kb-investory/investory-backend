package com.investory.global.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// GCP LB 헬스체크 및 배포 후 컨테이너 준비 상태 확인용. DB 등 외부 의존성 조회 없이
// 프로세스가 요청을 받을 수 있는 상태이면 바로 200을 반환한다. SecurityConfig의 permitAll 대상.
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
