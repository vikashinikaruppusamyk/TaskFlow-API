package com.example.taskflow.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * Answers unauthenticated requests to protected endpoints. The exception is handed to the MVC exception
 * resolvers, so a 401 has the same problem+json body as every other error the API returns.
 */
@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver exceptionResolver;

    public ProblemAuthenticationEntryPoint(@Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) {
        this.exceptionResolver = exceptionResolver;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        // Prefer the specific reason from the JWT filter (expired, tampered, ...) over Spring's generic one
        AuthenticationException cause = request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE)
                instanceof InvalidTokenException tokenError ? tokenError : authException;
        exceptionResolver.resolveException(request, response, null, cause);
    }
}
