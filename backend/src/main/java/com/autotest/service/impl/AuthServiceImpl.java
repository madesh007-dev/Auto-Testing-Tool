package com.autotest.service.impl;

import com.autotest.dto.AuthResponse;
import com.autotest.dto.LoginRequest;
import com.autotest.dto.RegisterRequest;
import com.autotest.entity.Role;
import com.autotest.entity.User;
import com.autotest.exception.BadRequestException;
import com.autotest.exception.UnauthorizedException;
import com.autotest.repository.UserRepository;
import com.autotest.service.AuthService;
import com.autotest.util.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email is already registered");
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = new User(
                request.username(),
                request.email(),
                encodedPassword,
                Role.ROLE_USER
        );

        User savedUser = userRepository.save(user);

        String token = tokenProvider.generateToken(
                savedUser.getUsername(),
                savedUser.getId(),
                savedUser.getRole().name()
        );

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid username or password");
        }

        String token = tokenProvider.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
