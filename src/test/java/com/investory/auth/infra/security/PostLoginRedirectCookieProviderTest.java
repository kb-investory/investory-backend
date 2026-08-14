package com.investory.auth.infra.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostLoginRedirectCookieProviderTest {

    private static final String DEFAULT_REDIRECT_URI = "http://localhost:5173";
    private static final String ALLOWED_ORIGINS = "http://localhost:5173,https://investory.kr";
    private static final String EXISTING_USER_REDIRECT_URI = "http://www.investory.kr/home";

    private PostLoginRedirectCookieProvider provider;

    @BeforeEach
    void setUp() {
        provider = new PostLoginRedirectCookieProvider();
        ReflectionTestUtils.setField(provider, "defaultRedirectUri", DEFAULT_REDIRECT_URI);
        ReflectionTestUtils.setField(provider, "allowedOriginsRaw", ALLOWED_ORIGINS);
        ReflectionTestUtils.setField(provider, "existingUserRedirectUri", EXISTING_USER_REDIRECT_URI);
        ReflectionTestUtils.setField(provider, "secure", false);
    }

    @Test
    void 기존_회원이면_프론트가_요청한_주소를_무시하고_고정_홈으로_보낸다() {
        String result = provider.resolveRedirectUri("https://investory.kr/some/deep/path", false);

        assertEquals(EXISTING_USER_REDIRECT_URI, result);
    }

    @Test
    void 신규_회원이고_허용된_origin이면_요청한_주소_그대로_보낸다() {
        String requested = "https://investory.kr/onboarding";

        String result = provider.resolveRedirectUri(requested, true);

        assertEquals(requested, result);
    }

    @Test
    void 신규_회원이고_허용되지_않은_origin이면_기본_주소로_대체한다() {
        String result = provider.resolveRedirectUri("https://evil.com/phishing", true);

        assertEquals(DEFAULT_REDIRECT_URI, result);
    }

    @Test
    void 신규_회원이고_주소가_비어있으면_기본_주소를_사용한다() {
        String result = provider.resolveRedirectUri(null, true);

        assertEquals(DEFAULT_REDIRECT_URI, result);
    }
}
