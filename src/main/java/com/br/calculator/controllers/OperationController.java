package com.br.calculator.controllers;

import com.br.calculator.dto.OperationRequest;
import com.br.calculator.dto.OperationResponse;
import com.br.calculator.dto.RecordResponse;
import com.br.calculator.dto.UserStatsResponse;
import com.br.calculator.entities.User;
import com.br.calculator.exceptions.OperationException;
import com.br.calculator.repositories.UserRepository;
import com.br.calculator.services.OperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/operation")
public class OperationController {

    private static final Logger logger = LoggerFactory.getLogger(OperationController.class);

    @Autowired
    private OperationService operationService;
    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Page<RecordResponse>> getOperations(
            Authentication authentication,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        String username = authentication.getName();
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            // Return BAD_REQUEST if user is not found
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        Pageable pageable = PageRequest.of(page, size);
        User user = userOptional.get();
        logger.info("Fetching user operations");
        Page<RecordResponse> userRecords = operationService.getUserRecords(user, pageable);

        return ResponseEntity.ok(userRecords);
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
            logger.info("Executing operation");
            OperationResponse operationResponse = operationService.executeOperation(operation, user);
            logger.info("Operation success");
            return ResponseEntity.ok(operationResponse);
        } catch (OperationException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

}
