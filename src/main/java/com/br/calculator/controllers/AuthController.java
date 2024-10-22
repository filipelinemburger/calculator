package com.br.calculator.controllers;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import com.br.calculator.entities.User;
import com.br.calculator.exceptions.UserException;
import com.br.calculator.security.jwt.JwtTokenProvider;
import com.br.calculator.services.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        Map<String, String> response = new HashMap<>();
        try {
            logger.info("Creating new user");
            userService.createNewUser(user);
            response.put("message", "User registered successfully!");
            return ResponseEntity.status(CREATED).body(response);
        } catch (DataAccessException ex) {
            return ResponseEntity.status(INTERNAL_SERVER_ERROR).body(ex.getMessage());
        } catch (UserException ex) {
            return ResponseEntity.status(BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            logger.info("Starting user authentication");
            var foundUser = userService.doLogin(user);
            String token = jwtTokenProvider.generateToken(foundUser.getUsername());
            Map<String, String> response = new HashMap<>();
            logger.info("User authenticated successfully");
            response.put("token", token);
            return ResponseEntity.ok(response);
        } catch (UserException ex) {
            return ResponseEntity.status(BAD_REQUEST).body(ex.getMessage());
        }
    }

}