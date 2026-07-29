package com.aakash.tradenest.user.controller;

import com.aakash.tradenest.user.dto.UserProfileResponse;
import com.aakash.tradenest.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserProfileResponse getMyProfile(@AuthenticationPrincipal UserDetails principal){

        return userService.getProfile(principal.getUsername());
    }
}
