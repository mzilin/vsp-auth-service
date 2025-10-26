package com.mariuszilinskas.streamix.auth.identity.controller;

import com.mariuszilinskas.streamix.auth.identity.dto.LoginRequest;
import com.mariuszilinskas.streamix.auth.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * This class provides REST APIs for handling CRUD operations related to authentication.
 *
 * @author Marius Zilinskas
 */
@RestController
@RequestMapping()
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /auth/login : Authenticates a user.
     */
    @PostMapping("/login")
    public ResponseEntity<Void> authenticateUser(
            @Valid @RequestBody LoginRequest request,
            @NonNull HttpServletResponse response
    ) {
        authService.authenticateUser(request, response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * POST /auth/token : Refreshes authentication tokens.
     */
    @PostMapping("/token")
    public ResponseEntity<Void> refreshTokens(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response
    ) {
        authService.refreshTokens(request, response);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * POST /auth/logout/{userId} : Logs out a user.
     */
    @PostMapping("/logout/{userId}")
    public ResponseEntity<Void> logoutUser(
            @PathVariable UUID userId,
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response
    ) {
        authService.logoutUser(request, response, userId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

}
