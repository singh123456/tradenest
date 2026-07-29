package com.aakash.tradenest.user.service;

import com.aakash.tradenest.user.dto.UserProfileResponse;
import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getProfile(String email){
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new IllegalArgumentException("Authenticated user not found: "+ email));
        return UserProfileResponse.from(user);
    }

}
