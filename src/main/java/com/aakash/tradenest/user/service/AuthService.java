package com.aakash.tradenest.user.service;

import com.aakash.tradenest.common.exception.EmailAlreadyExistsException;
import com.aakash.tradenest.security.JwtService;
import com.aakash.tradenest.user.dto.AuthResponse;
import com.aakash.tradenest.user.dto.LoginRequest;
import com.aakash.tradenest.user.dto.RegisterRequest;
import com.aakash.tradenest.user.entity.Role;
import com.aakash.tradenest.user.entity.User;
import com.aakash.tradenest.user.repository.UserRepository;
import com.aakash.tradenest.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final WalletService walletService;
    public AuthResponse register( RegisterRequest request) {
         if(userRepository.existsByEmail(request.email())){
             throw new EmailAlreadyExistsException(request.email());
         }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER)
                .build();

         User saved = userRepository.save(user);

         walletService.createWallet(saved);
         String token = jwtService.generateToken(saved);
         return AuthResponse.of(token, saved.getId(), saved.getName(), saved.getEmail());
    }

    public AuthResponse login(@Valid LoginRequest request) {

        authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.email(),request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(()-> new IllegalStateException("User authenticated but not found: " + request.email()));

        String token = jwtService.generateToken(user);
        return AuthResponse.of(token,user.getId(),user.getName(),user.getEmail());
    }
}
