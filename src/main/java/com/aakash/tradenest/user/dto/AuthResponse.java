package com.aakash.tradenest.user.dto;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String name,
        String email
) {
    public static AuthResponse of(String token,Long userId,String name, String email){
        return new AuthResponse(token, "Bearer", userId, name, email);
    }
}
