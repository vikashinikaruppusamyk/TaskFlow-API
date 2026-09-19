package com.example.taskflow.security;

import com.example.taskflow.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates requests that carry an {@code Authorization: Bearer <token>} header.
 * <p>
 * A bad token does not fail the request here: the reason is stored on the request and the request continues
 * unauthenticated. Public endpoints still work, and protected ones are rejected by
 * {@link ProblemAuthenticationEntryPoint}, which reports that reason back to the client.
 * <p>
 * Not a Spring bean on purpose, so it runs only inside the security filter chain and not a second time as a
 * plain servlet filter.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    static final String TOKEN_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".TOKEN_ERROR";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            try {
                authenticate(request, header.substring(BEARER_PREFIX.length()).trim());
            } catch (InvalidTokenException e) {
                SecurityContextHolder.clearContext();
                request.setAttribute(TOKEN_ERROR_ATTRIBUTE, e);
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String token) {
        AuthenticatedUser user = jwtService.parseToken(token);
        // A still-unexpired token stops working as soon as its account no longer exists
        if (!userRepository.existsById(user.id())) {
            throw new InvalidTokenException("Access token belongs to an account that no longer exists");
        }

        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(user, null, List.of());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
