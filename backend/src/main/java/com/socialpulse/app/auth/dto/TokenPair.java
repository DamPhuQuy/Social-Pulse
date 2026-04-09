package com.socialpulse.app.auth.dto;

/**
 * Pair of tokens produced after a successful login.
 *
 * accessToken  — short-lived (15 min), returned in JSON body for the FE to keep in memory.
 * refreshToken — long-lived (7 days), sent to the FE via HttpOnly cookie.
 */
public record TokenPair(String accessToken, String refreshToken) {}
