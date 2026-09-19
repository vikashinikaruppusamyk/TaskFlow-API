package com.example.taskflow.security;

/** The caller identified by a valid access token. Controllers receive it via {@code @AuthenticationPrincipal}. */
public record AuthenticatedUser(Long id, String email) {
}
