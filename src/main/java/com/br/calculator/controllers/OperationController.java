package com.br.calculator.controllers;

import com.br.calculator.dto.OperationRequest;
import com.br.calculator.dto.OperationResponse;
import com.br.calculator.dto.RecordResponse;
import com.br.calculator.dto.UserStatsResponse;
import com.br.calculator.entities.User;
import com.br.calculator.exceptions.OperationException;
import com.br.calculator.repositories.UserRepository;
import com.br.calculator.services.OperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/operation")
public class OperationController {

    @Autowired
    private OperationService operationService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<List<RecordResponse>> getOperations(Authentication authentication) {
        try {
            String userName = authentication.getName();
            User user = userRepository.findByUsername(userName).orElseThrow(() -> new RuntimeException("User not found"));
            List<RecordResponse> operations = operationService.getUserRecords(user);
            return ResponseEntity.ok(operations);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(new ArrayList<>());
        }
    }

    @GetMapping("/user-stats")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<UserStatsResponse> getUserStats(Authentication authentication) {
        try {
            String userName = authentication.getName();
            User user = userRepository.findByUsername(userName).orElseThrow(() -> new RuntimeException("User not found"));
            UserStatsResponse userStatsResponse = operationService.getUserStats(user);
            return ResponseEntity.ok().body(userStatsResponse);
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> calculate(Authentication authentication, @RequestBody OperationRequest operation) {
        try {
            String userName = authentication.getName();
            User user = userRepository.findByUsername(userName).orElseThrow(() -> new RuntimeException("User not found"));
            OperationResponse operationResponse = operationService.executeOperation(operation, user);
            return ResponseEntity.ok(operationResponse);
        } catch (OperationException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

}
