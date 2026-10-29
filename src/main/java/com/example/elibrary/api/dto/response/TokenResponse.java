package com.example.elibrary.api.dto.response;

import com.example.elibrary.domain.model.MemberRole;

/** Bearer token answer. Contains no permissions - those are always re-evaluated server side. */
public record TokenResponse(String accessToken,
                            String tokenType,
                            long expiresInSeconds,
                            long memberId,
                            MemberRole role) {

    public static TokenResponse of(String token, long expiresInSeconds, long memberId, MemberRole role) {
        return new TokenResponse(token, "Bearer", expiresInSeconds, memberId, role);
    }
}
