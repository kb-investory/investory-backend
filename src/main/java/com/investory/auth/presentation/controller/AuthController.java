package com.investory.auth.presentation.controller;

import com.investory.auth.domain.constant.OAuthProviderType;
import com.investory.auth.domain.exception.AuthErrorCode;
import com.investory.auth.domain.exception.AuthException;
import com.investory.auth.domain.services.AuthService;
import com.investory.auth.domain.services.dto.command.OAuthLoginCommand;
import com.investory.auth.domain.services.dto.command.ReissueTokenCommand;
import com.investory.auth.domain.services.dto.query.GetUserQuery;
import com.investory.auth.domain.services.dto.result.LoginResult;
import com.investory.auth.domain.services.dto.result.ReissueResult;
import com.investory.auth.infra.clients.google.GoogleOAuthClient;
import com.investory.auth.infra.clients.kakao.KakaoOAuthClient;
import com.investory.auth.infra.clients.naver.NaverOAuthClient;
import com.investory.auth.infra.security.OAuthStateCookieProvider;
import com.investory.auth.infra.security.PostLoginRedirectCookieProvider;
import com.investory.auth.infra.security.RefreshTokenCookieProvider;
import com.investory.auth.presentation.dto.response.ReissueResponse;
import com.investory.auth.presentation.dto.response.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final KakaoOAuthClient kakaoOAuthClient;
    private final NaverOAuthClient naverOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;
    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;
    private final OAuthStateCookieProvider oAuthStateCookieProvider;
    private final PostLoginRedirectCookieProvider postLoginRedirectCookieProvider;

    // 카카오 로그인 페이지로 Redirect시키는 Controller (카카오는 state 검증이 필수가 아니라 null로 넘김)
    // redirectUri: 로그인 끝난 뒤 최종적으로 돌아갈 프론트 주소. 안 주면 기본 프론트 주소를 쓴다.
    @GetMapping("/oauth/kakao/authorization")
    public ResponseEntity<Void> kakaoLogin(@RequestParam(required = false) String redirectUri) {
        String sanitizedRedirectUri = postLoginRedirectCookieProvider.sanitize(redirectUri);
        ResponseCookie redirectCookie = postLoginRedirectCookieProvider.create(sanitizedRedirectUri);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, redirectCookie.toString())
                .location(URI.create(kakaoOAuthClient.getAuthorizeUrl(null)))
                .build();
    }

    // 로그인 인증 후 발급된 Refresh Token을 HttpOnly Cookie로 저장하고,
    // authorization 요청 때 저장해둔 프론트 주소로 Redirection시키는 Controller
    @GetMapping("/oauth/kakao/callback")
    public ResponseEntity<Void> kakaoCallback(
            @RequestParam String code,
            @CookieValue(name = PostLoginRedirectCookieProvider.COOKIE_NAME, required = false) String savedRedirectUri) {

        LoginResult result = authService.login(new OAuthLoginCommand(OAuthProviderType.KAKAO, code, null));

        ResponseCookie refreshTokenCookie = refreshTokenCookieProvider.create(result.refreshToken());

        String redirectTo = postLoginRedirectCookieProvider.resolveRedirectUri(savedRedirectUri, result.newUser());
        ResponseCookie expiredRedirectCookie = postLoginRedirectCookieProvider.expire();

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRedirectCookie.toString())
                .header(HttpHeaders.LOCATION, redirectTo)
                .build();
    }

    // 네이버 로그인 페이지로 Redirect시키는 Controller.
    // 네이버는 state 검증이 필수라서, 여기서 state를 새로 발급해 (1) 인가 URL에 실어 보내고 (2) 짧은 만료의 쿠키에도 저장해둔다.
    // 콜백이 돌아오면 이 쿠키 값과 네이버가 되돌려준 state가 같은지 비교해서 CSRF 요청을 걸러낸다.
    // redirectUri: 로그인 끝난 뒤 최종적으로 돌아갈 프론트 주소. 안 주면 기본 프론트 주소를 쓴다.
    @GetMapping("/oauth/naver/authorization")
    public ResponseEntity<Void> naverLogin(@RequestParam(required = false) String redirectUri) {
        String state = oAuthStateCookieProvider.generateState();
        ResponseCookie stateCookie = oAuthStateCookieProvider.create(state);

        String sanitizedRedirectUri = postLoginRedirectCookieProvider.sanitize(redirectUri);
        ResponseCookie redirectCookie = postLoginRedirectCookieProvider.create(sanitizedRedirectUri);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, stateCookie.toString())
                .header(HttpHeaders.SET_COOKIE, redirectCookie.toString())
                .location(URI.create(naverOAuthClient.getAuthorizeUrl(state)))
                .build();
    }

    // 네이버 로그인 콜백. state 쿠키 값과 쿼리파라미터로 돌아온 state가 일치하는지 먼저 검증한 뒤 로그인을 진행한다.
    @GetMapping("/oauth/naver/callback")
    public ResponseEntity<Void> naverCallback(
            @RequestParam String code,
            @RequestParam String state,
            @CookieValue(name = OAuthStateCookieProvider.COOKIE_NAME, required = false) String savedState,
            @CookieValue(name = PostLoginRedirectCookieProvider.COOKIE_NAME, required = false) String savedRedirectUri) {

        if (!StringUtils.hasText(savedState) || !savedState.equals(state)) {
            log.warn("Naver OAuth state mismatch: savedStatePresent={}, stateEquals={}",
                    StringUtils.hasText(savedState), savedState != null && savedState.equals(state));
            throw new AuthException(AuthErrorCode.OAUTH_STATE_MISMATCH);
        }

        LoginResult result = authService.login(new OAuthLoginCommand(OAuthProviderType.NAVER, code, state));

        ResponseCookie refreshTokenCookie = refreshTokenCookieProvider.create(result.refreshToken());
        ResponseCookie expiredStateCookie = oAuthStateCookieProvider.expire(); // 검증 끝났으니 state 쿠키는 바로 지운다
        ResponseCookie expiredRedirectCookie = postLoginRedirectCookieProvider.expire();
        String redirectTo = postLoginRedirectCookieProvider.resolveRedirectUri(savedRedirectUri, result.newUser());

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredStateCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRedirectCookie.toString())
                .header(HttpHeaders.LOCATION, redirectTo)
                .build();
    }

    // 구글 로그인 페이지로 Redirect시키는 Controller (구글도 state 검증이 필수가 아니라 null로 넘김)
    // redirectUri: 로그인 끝난 뒤 최종적으로 돌아갈 프론트 주소. 안 주면 기본 프론트 주소를 쓴다.
    @GetMapping("/oauth/google/authorization")
    public ResponseEntity<Void> googleLogin(@RequestParam(required = false) String redirectUri) {
        String sanitizedRedirectUri = postLoginRedirectCookieProvider.sanitize(redirectUri);
        ResponseCookie redirectCookie = postLoginRedirectCookieProvider.create(sanitizedRedirectUri);

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, redirectCookie.toString())
                .location(URI.create(googleOAuthClient.getAuthorizeUrl(null)))
                .build();
    }

    // 구글 로그인 콜백. 카카오와 동일하게 발급된 Refresh Token을 HttpOnly Cookie로 저장하고
    // authorization 요청 때 저장해둔 프론트 주소로 Redirect시킨다.
    @GetMapping("/oauth/google/callback")
    public ResponseEntity<Void> googleCallback(
            @RequestParam String code,
            @CookieValue(name = PostLoginRedirectCookieProvider.COOKIE_NAME, required = false) String savedRedirectUri) {

        LoginResult result = authService.login(new OAuthLoginCommand(OAuthProviderType.GOOGLE, code, null));

        ResponseCookie refreshTokenCookie = refreshTokenCookieProvider.create(result.refreshToken());
        ResponseCookie expiredRedirectCookie = postLoginRedirectCookieProvider.expire();
        String redirectTo = postLoginRedirectCookieProvider.resolveRedirectUri(savedRedirectUri, result.newUser());

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, expiredRedirectCookie.toString())
                .header(HttpHeaders.LOCATION, redirectTo)
                .build();
    }

    // Cookie의 RefreshToken을 사용해 AccessToken을 재발급하여 Json으로 반환하는 Controller
    @PostMapping("/token/refresh")
    public ResponseEntity<ReissueResponse> reissue(
            @CookieValue(name = RefreshTokenCookieProvider.COOKIE_NAME, required = false) String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REQUEST);
        }
        ReissueResult result = authService.reissue(new ReissueTokenCommand(refreshToken));
        return ResponseEntity.ok(ReissueResponse.from(result));
    }

    // refreshToken 쿠키를 즉시 만료시켜 로그아웃 처리하는 Controller (accessToken은 클라이언트가 별도로 폐기해야 함)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        SecurityContextHolder.clearContext();
        ResponseCookie expiredCookie = refreshTokenCookieProvider.expire();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }

    // SecurityContext에 담긴(JwtAuthenticationFilter가 채운) userId로 현재 로그인한 회원 정보를 조회하는 Controller
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        Long userId = (Long) authentication.getPrincipal();
        var result = authService.getMe(new GetUserQuery(userId));
        return ResponseEntity.ok(UserResponse.from(result));
    }

    // 계정 탈퇴 Controller. 로그아웃과 동일하게 refreshToken 쿠키를 즉시 만료시킨다(accessToken은
    // 클라이언트가 별도로 폐기해야 함).
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        Long userId = (Long) authentication.getPrincipal();
        authService.withdraw(userId);

        SecurityContextHolder.clearContext();
        ResponseCookie expiredCookie = refreshTokenCookieProvider.expire();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString())
                .build();
    }
}
