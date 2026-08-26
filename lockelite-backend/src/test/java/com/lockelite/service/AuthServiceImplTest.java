package com.lockelite.service;

import com.lockelite.audit.AuditLogService;
import com.lockelite.dto.request.LoginRequest;
import com.lockelite.dto.request.RegisterRequest;
import com.lockelite.exception.BusinessException;
import com.lockelite.model.OtpToken;
import com.lockelite.model.User;
import com.lockelite.repository.OtpTokenRepository;
import com.lockelite.repository.UserRepository;
import com.lockelite.security.JwtUtil;
import com.lockelite.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl — Sign Up & Sign In
 * Uses JUnit 5 + Mockito (no Spring context loaded → fast tests)
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    // ── Mocks ────────────────────────────────────────────────────────────
    @Mock private UserRepository      userRepository;
    @Mock private OtpTokenRepository  otpTokenRepository;
    @Mock private PasswordEncoder     passwordEncoder;
    @Mock private JwtUtil             jwtUtil;
    @Mock private EmailService        emailService;
    @Mock private AuditLogService     auditLogService;

    @InjectMocks
    private AuthServiceImpl authService;

    // ── Helper builders ──────────────────────────────────────────────────

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Prasad Mane");
        req.setEmail("prasad@test.com");
        req.setUsername("prasadmane");
        req.setPassword("Password@123");
        req.setPhoneNumber("9876543210");
        req.setDateOfBirth(LocalDate.of(2000, 1, 1));
        req.setTermsAccepted(true);
        return req;
    }

    private User savedUser() {
        return User.builder()
                .id(1L)
                .fullName("Prasad Mane")
                .email("prasad@test.com")
                .username("prasadmane")
                .passwordHash("$2a$hashed")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .emailVerified(false)
                .passwordChanged(true)
                .build();
    }

    private LoginRequest loginRequest(String emailOrUsername, String password) {
        LoginRequest req = new LoginRequest();
        req.setEmailOrUsername(emailOrUsername);
        req.setPassword(password);
        return req;
    }

    // ════════════════════════════════════════════════════════════════════
    // SIGN UP TESTS
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Sign Up — register()")
    class SignUpTests {

        @Test
        @DisplayName("✅ Successful registration sends OTP email")
        void register_success() {
            // Arrange
            RegisterRequest req = validRegisterRequest();
            User user = savedUser();

            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(req.getUsername())).thenReturn(false);
            when(passwordEncoder.encode(req.getPassword())).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(otpTokenRepository.save(any(OtpToken.class))).thenAnswer(i -> i.getArgument(0));
            doNothing().when(emailService).sendOtpEmail(anyString(), anyString(), anyString());
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any());

            // Act — should not throw
            assertThatNoException().isThrownBy(() -> authService.register(req));

            // Assert
            verify(userRepository).save(any(User.class));
            verify(emailService).sendOtpEmail(eq("prasad@test.com"), eq("Prasad Mane"), anyString());
            verify(auditLogService).log(any(), eq("USER_REGISTERED"), eq("User"), any(), any(), any(), any());
        }

        @Test
        @DisplayName("❌ Registration fails when terms not accepted")
        void register_termsNotAccepted_throwsException() {
            RegisterRequest req = validRegisterRequest();
            req.setTermsAccepted(false);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("terms");

            verifyNoInteractions(userRepository, emailService);
        }

        @Test
        @DisplayName("❌ Registration fails when email already exists")
        void register_duplicateEmail_throwsException() {
            RegisterRequest req = validRegisterRequest();
            when(userRepository.existsByEmail(req.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Email already registered");

            verify(userRepository, never()).save(any());
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("❌ Registration fails when username already taken")
        void register_duplicateUsername_throwsException() {
            RegisterRequest req = validRegisterRequest();
            when(userRepository.existsByEmail(req.getEmail())).thenReturn(false);
            when(userRepository.existsByUsername(req.getUsername())).thenReturn(true);

            assertThatThrownBy(() -> authService.register(req))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Username already taken");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("✅ Password is encoded before saving")
        void register_passwordIsEncoded() {
            RegisterRequest req = validRegisterRequest();
            User user = savedUser();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode("Password@123")).thenReturn("$2a$encoded");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(otpTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            doNothing().when(emailService).sendOtpEmail(any(), any(), any());
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any());

            authService.register(req);

            // Verify encoder was called with raw password
            verify(passwordEncoder).encode("Password@123");
            // Verify raw password is NOT saved
            verify(userRepository).save(argThat(u -> !u.getPasswordHash().equals("Password@123")));
        }

        @Test
        @DisplayName("✅ New user is saved with CUSTOMER role")
        void register_userSavedWithCustomerRole() {
            RegisterRequest req = validRegisterRequest();
            User user = savedUser();

            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("$2a$hashed");
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(otpTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            doNothing().when(emailService).sendOtpEmail(any(), any(), any());
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any());

            authService.register(req);

            verify(userRepository).save(argThat(u ->
                    u.getRole() == User.Role.CUSTOMER &&
                    !u.getEmailVerified() // email not yet verified
            ));
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // SIGN IN TESTS
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Sign In — login()")
    class SignInTests {

        @BeforeEach
        void setupAuditLog() {
            // Prevent strict stubbing failure for audit log in all login tests
            lenient().doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("✅ Successful login returns JWT token and user info")
        void login_success_returnsToken() {
            User user = savedUser();
            user.setEmailVerified(true);
            user.setPasswordChanged(true);

            when(userRepository.findByEmailOrUsername("prasad@test.com", "prasad@test.com"))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Password@123", "$2a$hashed")).thenReturn(true);
            when(jwtUtil.generateToken(user)).thenReturn("mock.jwt.token");

            Map<String, Object> result = authService.login(loginRequest("prasad@test.com", "Password@123"));

            assertThat(result).containsKey("token");
            assertThat(result.get("token")).isEqualTo("mock.jwt.token");
            assertThat(result.get("role")).isEqualTo("CUSTOMER");
            verify(jwtUtil).generateToken(user);
        }

        @Test
        @DisplayName("❌ Login fails with wrong password")
        void login_wrongPassword_throwsException() {
            User user = savedUser();
            user.setEmailVerified(true);

            when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("wrongpass", "$2a$hashed")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(loginRequest("prasad@test.com", "wrongpass")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid credentials");

            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("❌ Login fails when user not found")
        void login_userNotFound_throwsException() {
            when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(loginRequest("unknown@test.com", "Password@123")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid credentials");

            verifyNoInteractions(passwordEncoder, jwtUtil);
        }

        @Test
        @DisplayName("❌ Login fails when email not verified")
        void login_emailNotVerified_throwsException() {
            User user = savedUser();
            user.setEmailVerified(false); // not verified

            when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest("prasad@test.com", "Password@123")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("verify your email");

            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("❌ Login fails when account is deactivated")
        void login_accountDeactivated_throwsException() {
            User user = savedUser();
            user.setEmailVerified(true);
            user.setIsActive(false); // deactivated

            when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

            assertThatThrownBy(() -> authService.login(loginRequest("prasad@test.com", "Password@123")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("deactivated");

            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("✅ Login works with username instead of email")
        void login_withUsername_success() {
            User user = savedUser();
            user.setEmailVerified(true);

            when(userRepository.findByEmailOrUsername("prasadmane", "prasadmane"))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches("Password@123", "$2a$hashed")).thenReturn(true);
            when(jwtUtil.generateToken(user)).thenReturn("mock.jwt.token");

            Map<String, Object> result = authService.login(loginRequest("prasadmane", "Password@123"));

            assertThat(result.get("token")).isEqualTo("mock.jwt.token");
        }

        @Test
        @DisplayName("✅ Login response contains branchId and userId")
        void login_responseContainsCorrectFields() {
            User user = savedUser();
            user.setEmailVerified(true);
            user.setBranchId(5L);

            when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtil.generateToken(user)).thenReturn("mock.jwt.token");

            Map<String, Object> result = authService.login(loginRequest("prasad@test.com", "Password@123"));

            assertThat(result).containsKeys("token", "role", "branchId", "userId", "passwordChanged");
            assertThat(result.get("userId")).isEqualTo(1L);
            assertThat(result.get("branchId")).isEqualTo(5L);
        }

        @Test
        @DisplayName("✅ Audit log is created on successful login")
        void login_auditLogCreated() {
            User user = savedUser();
            user.setEmailVerified(true);

            when(userRepository.findByEmailOrUsername(anyString(), anyString()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
            when(jwtUtil.generateToken(user)).thenReturn("mock.jwt.token");

            authService.login(loginRequest("prasad@test.com", "Password@123"));

            verify(auditLogService).log(eq(1L), eq("USER_LOGIN"), eq("User"), eq(1L), isNull(), eq("CUSTOMER"), isNull());
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // OTP VERIFICATION TESTS
    // ════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OTP Verification — verifyOtp()")
    class OtpVerificationTests {

        @Test
        @DisplayName("✅ Valid OTP verifies email successfully")
        void verifyOtp_success() {
            User user = savedUser();
            OtpToken token = OtpToken.builder()
                    .id(1L).userId(1L).otp("123456")
                    .type(OtpToken.OtpType.EMAIL)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .used(false).build();

            when(userRepository.findByEmail("prasad@test.com")).thenReturn(Optional.of(user));
            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(1L, OtpToken.OtpType.EMAIL))
                    .thenReturn(Optional.of(token));
            when(userRepository.save(any())).thenReturn(user);
            when(otpTokenRepository.save(any())).thenReturn(token);
            doNothing().when(auditLogService).log(any(), any(), any(), any(), any(), any(), any());

            assertThatNoException().isThrownBy(() -> authService.verifyOtp("prasad@test.com", "123456"));

            verify(userRepository).save(argThat(u -> u.getEmailVerified()));
        }

        @Test
        @DisplayName("❌ Expired OTP throws exception")
        void verifyOtp_expired_throwsException() {
            User user = savedUser();
            OtpToken token = OtpToken.builder()
                    .id(1L).userId(1L).otp("123456")
                    .type(OtpToken.OtpType.EMAIL)
                    .expiresAt(LocalDateTime.now().minusMinutes(1)) // expired
                    .used(false).build();

            when(userRepository.findByEmail("prasad@test.com")).thenReturn(Optional.of(user));
            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(1L, OtpToken.OtpType.EMAIL))
                    .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyOtp("prasad@test.com", "123456"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("❌ Wrong OTP throws exception")
        void verifyOtp_wrongOtp_throwsException() {
            User user = savedUser();
            OtpToken token = OtpToken.builder()
                    .id(1L).userId(1L).otp("123456")
                    .type(OtpToken.OtpType.EMAIL)
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .used(false).build();

            when(userRepository.findByEmail("prasad@test.com")).thenReturn(Optional.of(user));
            when(otpTokenRepository.findTopByUserIdAndTypeAndUsedFalseOrderByCreatedAtDesc(1L, OtpToken.OtpType.EMAIL))
                    .thenReturn(Optional.of(token));

            assertThatThrownBy(() -> authService.verifyOtp("prasad@test.com", "999999"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Invalid OTP");
        }
    }
}
