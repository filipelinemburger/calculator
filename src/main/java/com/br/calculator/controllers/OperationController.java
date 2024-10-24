package com.br.calculator.controllers;

import com.br.calculator.dto.OperationRequest;
import com.br.calculator.dto.OperationResponse;
import com.br.calculator.dto.RecordResponse;
import com.br.calculator.dto.UserStatsResponse;
import com.br.calculator.entities.User;
import com.br.calculator.exceptions.OperationException;
import com.br.calculator.exceptions.UserException;
import com.br.calculator.services.OperationService;
import com.br.calculator.services.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operation")
public class OperationController {

    private static final Logger logger = LoggerFactory.getLogger(OperationController.class);

    @Autowired
    private OperationService operationService;

    @Autowired
    private UserService userService;

    // Fetch authenticated user's operations
    @GetMapping
    public ResponseEntity<Page<RecordResponse>> getOperations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        User user = getAuthenticatedUser();
        Pageable pageable = PageRequest.of(page, size);
        logger.info("Fetching user operations for user: {}", user.getUsername());
        Page<RecordResponse> userRecords = operationService.getUserRecords(user, pageable);

        return ResponseEntity.ok(userRecords);
    }

    // Fetch authenticated user's statistics
    @GetMapping("/user-stats")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> getUserStats() {
        try {
            User user = getAuthenticatedUser();
            UserStatsResponse userStatsResponse = operationService.getUserStats(user);
            return ResponseEntity.ok().body(userStatsResponse);
        } catch (UserException ex) {
            logger.error("User validation error", ex);
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (RuntimeException ex) {
            logger.error("Error fetching user stats", ex);
            return ResponseEntity.badRequest().body(null);
        }
    }

    // Perform operation for authenticated user
    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<?> calculate(@RequestBody OperationRequest operation) {
        try {
            User user = getAuthenticatedUser();
            logger.info("Executing operation for user: {}", user.getUsername());
            OperationResponse operationResponse = operationService.executeOperation(operation, user);
            logger.info("Operation success");
            return ResponseEntity.ok(operationResponse);
        } catch (OperationException ex) {
            logger.error("Error during operation execution", ex);
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (UserException ex) {
            logger.error("User validation error", ex);
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    private boolean isUserAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails;
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isUserAuthenticated(authentication)) {
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            return userService.findUserByUserName(userDetails.getUsername());
        }
        throw new UserException("User not authenticated");
    }

}
