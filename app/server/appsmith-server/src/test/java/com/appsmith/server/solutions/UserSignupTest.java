package com.appsmith.server.solutions;

import com.appsmith.server.authentication.handlers.AuthenticationSuccessHandler;
import com.appsmith.server.configurations.CommonConfig;
import com.appsmith.server.domains.Tenant;
import com.appsmith.server.domains.TenantConfiguration;
import com.appsmith.server.domains.User;
import com.appsmith.server.exceptions.AppsmithError;
import com.appsmith.server.exceptions.AppsmithException;
import com.appsmith.server.helpers.NetworkUtils;
import com.appsmith.server.helpers.UserUtils;
import com.appsmith.server.services.AnalyticsService;
import com.appsmith.server.services.CaptchaService;
import com.appsmith.server.services.ConfigService;
import com.appsmith.server.services.EmailService;
import com.appsmith.server.services.TenantService;
import com.appsmith.server.services.UserDataService;
import com.appsmith.server.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static com.appsmith.server.helpers.ValidationUtils.LOGIN_PASSWORD_MAX_LENGTH;
import static com.appsmith.server.helpers.ValidationUtils.LOGIN_PASSWORD_MIN_LENGTH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
public class UserSignupTest {
    @MockBean
    private UserService userService;

    @MockBean
    private UserDataService userDataService;

    @MockBean
    private CaptchaService captchaService;

    @MockBean
    private AuthenticationSuccessHandler authenticationSuccessHandler;

    @MockBean
    private ConfigService configService;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private EnvManager envManager;

    @MockBean
    private CommonConfig commonConfig;

    @MockBean
    private UserUtils userUtils;

    @MockBean
    private NetworkUtils networkUtils;

    @MockBean
    private EmailService emailService;

    @MockBean
    private TenantService tenantService;

    private UserSignup userSignup;

    private static Tenant tenant;

    private static ServerWebExchange exchange;

    @BeforeEach
    public void setup() {
        userSignup = new UserSignupImpl(
                userService,
                userDataService,
                captchaService,
                authenticationSuccessHandler,
                configService,
                analyticsService,
                envManager,
                commonConfig,
                userUtils,
                networkUtils,
                emailService,
                tenantService);

        exchange = Mockito.mock(ServerWebExchange.class);
        tenant = new Tenant();
        TenantConfiguration configuration = new TenantConfiguration();
        tenant.setTenantConfiguration(configuration);
        Mockito.when(tenantService.getDefaultTenant()).thenReturn(Mono.just(tenant));
    }

    private String createRandomString(int length) {
        return "Z".repeat(Math.max(0, length));
    }

    @Test
    public void signupAndLogin_WhenPasswordTooShort_RaisesException() {
        User user = new User();
        user.setEmail("testemail@test123.com");
        user.setPassword(createRandomString(LOGIN_PASSWORD_MIN_LENGTH - 1));

        Mono<User> userMono = userSignup.signupAndLogin(user, exchange);
        StepVerifier.create(userMono)
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppsmithException);
                    String expectedErrorMessage = AppsmithError.INVALID_PASSWORD_LENGTH.getMessage(
                            LOGIN_PASSWORD_MIN_LENGTH, LOGIN_PASSWORD_MAX_LENGTH);
                    assertEquals(expectedErrorMessage, error.getMessage());
                })
                .verify();
    }

    @Test
    public void signupAndLogin_WhenPasswordTooLong_RaisesException() {
        User user = new User();
        user.setEmail("testemail@test123.com");
        user.setPassword(createRandomString(LOGIN_PASSWORD_MAX_LENGTH + 1));

        Mono<User> userMono = userSignup.signupAndLogin(user, exchange);
        StepVerifier.create(userMono)
                .expectErrorSatisfies(error -> {
                    assertTrue(error instanceof AppsmithException);

                    String expectedErrorMessage = AppsmithError.INVALID_PASSWORD_LENGTH.getMessage(
                            LOGIN_PASSWORD_MIN_LENGTH, LOGIN_PASSWORD_MAX_LENGTH);
                    assertEquals(expectedErrorMessage, error.getMessage());
                })
                .verify();
    }

    @Test
    public void signupAndLogin_whenStrongPasswordRequirementEnabled_whenPasswordIsWeak_RaisesException() {
        User user = new User();
        user.setEmail("testemail@test123.com");
        user.setPassword(createRandomString(LOGIN_PASSWORD_MAX_LENGTH - 1));

        tenant.getTenantConfiguration().setIsStrongPasswordPolicyEnabled(true);
        Mockito.when(tenantService.getDefaultTenant()).thenReturn(Mono.just(tenant));
        Mono<User> userMono = userSignup.signupAndLogin(user, exchange);
        StepVerifier.create(userMono)
                .expectErrorSatisfies(throwable -> {
                    assertTrue(throwable instanceof AppsmithException);
                    assertEquals(
                            AppsmithError.INSUFFICIENT_PASSWORD_STRENGTH.getMessage(
                                    LOGIN_PASSWORD_MIN_LENGTH, LOGIN_PASSWORD_MAX_LENGTH),
                            throwable.getMessage());
                })
                .verify();

        tenant.getTenantConfiguration().setIsStrongPasswordPolicyEnabled(false);
    }
}
