package org.example.passpoint.domain.auth.service;

import org.example.passpoint.domain.auth.client.GoogleOAuthClient;
import org.example.passpoint.domain.auth.client.KakaoOAuthClient;
import org.example.passpoint.domain.auth.dto.request.EmailSignupRequest;
import org.example.passpoint.domain.auth.dto.response.KakaoUserInfo;
import org.example.passpoint.domain.auth.dto.response.TokenResponse;
import org.example.passpoint.domain.user.entity.OAuthProvider;
import org.example.passpoint.domain.user.entity.User;
import org.example.passpoint.domain.user.repository.UserRepository;
import org.example.passpoint.global.exception.auth.InvalidCredentialsException;
import org.example.passpoint.global.exception.user.DuplicateEmailException;
import org.example.passpoint.global.jwt.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * AuthService의 카카오 로그인, 이메일 로그인/회원가입 분기 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private GoogleOAuthClient googleOAuthClient;
    @Mock
    private KakaoOAuthClient kakaoOAuthClient;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtProvider jwtProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private static final Long USER_ID = 1L;
    private static final Long NEW_USER_ID = 2L;
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REFRESH_TOKEN = "refresh-token";

    /** 토큰 발급(issueTokens) 경로까지 도달하는 성공 케이스에서만 사용 - 실패 케이스에서 stub하면 미사용 stubbing 에러가 난다 */
    private void stubTokenIssuance() {
        given(jwtProvider.createAccessToken(anyLong())).willReturn(ACCESS_TOKEN);
        given(jwtProvider.createRefreshToken(anyLong())).willReturn(REFRESH_TOKEN);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
    }

    private User existingUser(OAuthProvider provider, String oauthId, String email, String nickname, Long id) {
        User user = User.builder()
                .oauthProvider(provider)
                .oauthId(oauthId)
                .email(email)
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void 카카오로그인_기존회원이면_가입없이토큰을발급한다() {
        User user = existingUser(OAuthProvider.KAKAO, "kakao-1", "kakao@test.com", "카카오유저", USER_ID);
        given(kakaoOAuthClient.getUserInfo(ACCESS_TOKEN))
                .willReturn(new KakaoUserInfo("kakao-1", "kakao@test.com", "카카오유저"));
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.KAKAO, "kakao-1"))
                .willReturn(Optional.of(user));
        stubTokenIssuance();

        TokenResponse response = authService.loginWithKakao(ACCESS_TOKEN);

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(response.refreshToken()).isEqualTo(REFRESH_TOKEN);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 카카오로그인_신규회원이면_가입후토큰을발급한다() {
        given(kakaoOAuthClient.getUserInfo(ACCESS_TOKEN))
                .willReturn(new KakaoUserInfo("kakao-2", "new@kakao.com", "신규카카오유저"));
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.KAKAO, "kakao-2"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", NEW_USER_ID);
            return saved;
        });
        stubTokenIssuance();

        TokenResponse response = authService.loginWithKakao(ACCESS_TOKEN);

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getOauthProvider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(captor.getValue().getEmail()).isEqualTo("new@kakao.com");
        assertThat(captor.getValue().getNickname()).isEqualTo("신규카카오유저");
    }

    @Test
    void 카카오로그인_이메일동의안한신규회원이면_임시이메일로가입한다() {
        given(kakaoOAuthClient.getUserInfo(ACCESS_TOKEN))
                .willReturn(new KakaoUserInfo("kakao-3", null, null));
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.KAKAO, "kakao-3"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", NEW_USER_ID);
            return saved;
        });
        stubTokenIssuance();

        authService.loginWithKakao(ACCESS_TOKEN);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("kakao_kakao-3@kakao.local");
        assertThat(captor.getValue().getNickname()).isEqualTo("카카오사용자");
    }

    @Test
    void 이메일로그인_이메일과비밀번호가일치하면_대소문자무관하게조회해토큰을발급한다() {
        User user = existingUser(OAuthProvider.EMAIL, "test@example.com", "test@example.com", "tester", USER_ID);
        ReflectionTestUtils.setField(user, "password", "encoded-pw");
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.EMAIL, "test@example.com"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("password1234", "encoded-pw")).willReturn(true);
        stubTokenIssuance();

        TokenResponse response = authService.loginWithEmail("Test@Example.com", "password1234");

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        verify(userRepository).findByOauthProviderAndOauthId(OAuthProvider.EMAIL, "test@example.com");
    }

    @Test
    void 이메일로그인_가입되지않은이메일이면_InvalidCredentialsException() {
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.EMAIL, "nobody@test.com"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.loginWithEmail("nobody@test.com", "password1234"))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(passwordEncoder, jwtProvider);
    }

    @Test
    void 이메일로그인_비밀번호가틀리면_InvalidCredentialsException() {
        User user = existingUser(OAuthProvider.EMAIL, "test@example.com", "test@example.com", "tester", USER_ID);
        ReflectionTestUtils.setField(user, "password", "encoded-pw");
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.EMAIL, "test@example.com"))
                .willReturn(Optional.of(user));
        given(passwordEncoder.matches("wrong-password", "encoded-pw")).willReturn(false);

        assertThatThrownBy(() -> authService.loginWithEmail("test@example.com", "wrong-password"))
                .isInstanceOf(InvalidCredentialsException.class);

        verifyNoInteractions(jwtProvider);
    }

    @Test
    void 이메일회원가입_신규이메일이면_비밀번호를암호화해저장하고토큰을발급한다() {
        EmailSignupRequest request = new EmailSignupRequest("New@Test.com", "password1234", "newbie");
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.EMAIL, "new@test.com"))
                .willReturn(Optional.empty());
        given(passwordEncoder.encode("password1234")).willReturn("encoded-pw");
        given(userRepository.save(any(User.class))).willAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", NEW_USER_ID);
            return saved;
        });
        stubTokenIssuance();

        TokenResponse response = authService.signupWithEmail(request);

        assertThat(response.accessToken()).isEqualTo(ACCESS_TOKEN);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getOauthProvider()).isEqualTo(OAuthProvider.EMAIL);
        assertThat(saved.getOauthId()).isEqualTo("new@test.com");
        assertThat(saved.getEmail()).isEqualTo("new@test.com");
        assertThat(saved.getNickname()).isEqualTo("newbie");
        assertThat(saved.getPassword()).isEqualTo("encoded-pw");
    }

    @Test
    void 이메일회원가입_이미가입된이메일이면_DuplicateEmailException() {
        EmailSignupRequest request = new EmailSignupRequest("test@example.com", "password1234", "tester");
        User existing = existingUser(OAuthProvider.EMAIL, "test@example.com", "test@example.com", "tester", USER_ID);
        given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.EMAIL, "test@example.com"))
                .willReturn(Optional.of(existing));

        assertThatThrownBy(() -> authService.signupWithEmail(request))
                .isInstanceOf(DuplicateEmailException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtProvider);
    }
}
