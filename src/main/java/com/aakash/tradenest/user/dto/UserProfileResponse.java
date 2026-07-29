package com.aakash.tradenest.user.dto;

import com.aakash.tradenest.user.entity.User;

import java.time.Instant;

public record UserProfileResponse(
        Long id,
        String name,
        String email,
        String role,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserProfileResponse from(User user){
        return new UserProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

}
