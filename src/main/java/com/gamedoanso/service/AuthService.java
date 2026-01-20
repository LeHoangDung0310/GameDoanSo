package com.gamedoanso.service;

import com.gamedoanso.dto.AuthRequest;
import com.gamedoanso.dto.AuthResponse;
import com.gamedoanso.entity.User;
import com.gamedoanso.repository.UserRepository;
import com.gamedoanso.security.JwtService;

import java.util.Collections;
import lombok.RequiredArgsConstructor;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

        private final UserRepository repository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;

        public AuthResponse register(AuthRequest request) {
                if (repository.existsByUsername(request.getUsername())) {
                        throw new RuntimeException("Username already exists");
                }
                var user = User.builder()
                                .username(request.getUsername())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .score(0)
                                .turns(0)
                                .build();
                repository.save(user);

                var userDetails = new org.springframework.security.core.userdetails.User(
                                user.getUsername(),
                                user.getPassword(),
                                Collections.singletonList(new SimpleGrantedAuthority("USER")));

                var jwtToken = jwtService.generateToken(userDetails);
                return AuthResponse.builder()
                                .token(jwtToken)
                                .build();
        }

        public AuthResponse login(AuthRequest request) {
                var user = repository.findByUsername(request.getUsername())
                                .orElseThrow(() -> new RuntimeException("User not found"));
                if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        throw new RuntimeException("Invalid password");
                }
                var userDetails = new org.springframework.security.core.userdetails.User(
                                user.getUsername(),
                                user.getPassword(),
                                Collections.singletonList(new SimpleGrantedAuthority("USER")));

                var jwtToken = jwtService.generateToken(userDetails);
                return AuthResponse.builder()
                                .token(jwtToken)
                                .build();
        }
}
