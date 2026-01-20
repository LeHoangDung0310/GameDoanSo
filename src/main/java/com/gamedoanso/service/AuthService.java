package com.gamedoanso.service;

import com.gamedoanso.dto.AuthRequest;
import com.gamedoanso.dto.AuthResponse;
import com.gamedoanso.entity.User;
import com.gamedoanso.repository.UserRepository;
import com.gamedoanso.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(AuthRequest request) {
        var user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .score(0)
                .turns(10) // Initial turns
                .build();
        repository.save(user);
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("USER")
                .build();
        var jwtToken = jwtService.generateToken(userDetails);
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }

    public AuthResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()));
        var user = repository.findByUsername(request.getUsername())
                .orElseThrow();
        var userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("USER")
                .build();
        var jwtToken = jwtService.generateToken(userDetails);
        return AuthResponse.builder()
                .token(jwtToken)
                .build();
    }
}
