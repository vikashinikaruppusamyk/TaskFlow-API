package com.example.taskflow.security;

import com.example.taskflow.MutableClock;
import com.example.taskflow.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-at-least-32-characters";

    private final MutableClock clock = new MutableClock();
    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1), "taskflow-api"), clock);

    private static User user(long id, String email) {
        User user = new User(email, "hash", MutableClock.START);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void tokenRoundTripsUserIdAndEmail() {
        String token = jwtService.generateToken(user(42, "alex@example.com"));

        assertThat(jwtService.parseToken(token)).isEqualTo(new AuthenticatedUser(42L, "alex@example.com"));
    }

    @Test
    void tokenIsValidUntilItExpires() {
        String token = jwtService.generateToken(user(1, "alex@example.com"));

        clock.advance(Duration.ofMinutes(59));
        assertThat(jwtService.parseToken(token).id()).isEqualTo(1L);

        clock.advance(Duration.ofMinutes(2));
        assertThatThrownBy(() -> jwtService.parseToken(token))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateToken(user(1, "alex@example.com"));
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"2\",\"iss\":\"taskflow-api\"}".getBytes());

        assertThatThrownBy(() -> jwtService.parseToken(parts[0] + "." + forgedPayload + "." + parts[2]))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Access token is invalid");
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtService otherService = new JwtService(
                new JwtProperties("another-secret-that-is-also-32-characters-long", Duration.ofHours(1), "taskflow-api"), clock);
        String foreignToken = otherService.generateToken(user(1, "alex@example.com"));

        assertThatThrownBy(() -> jwtService.parseToken(foreignToken)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtService otherIssuer = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1), "someone-else"), clock);
        String token = otherIssuer.generateToken(user(1, "alex@example.com"));

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void garbageIsRejected() {
        assertThatThrownBy(() -> jwtService.parseToken("not-a-token")).isInstanceOf(InvalidTokenException.class);
        assertThatThrownBy(() -> jwtService.parseToken("")).isInstanceOf(InvalidTokenException.class);
    }
}
