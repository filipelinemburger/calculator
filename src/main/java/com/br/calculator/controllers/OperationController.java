package com.br.calculator.controllers;

import com.br.calculator.dto.OperationRequest;
import com.br.calculator.dto.OperationResponse;
import com.br.calculator.entities.User;
import com.br.calculator.repositories.UserRepository;
import com.br.calculator.services.OperationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operation")
public class OperationController {

    @Autowired
    private OperationService operationService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/calculate")
    @ResponseStatus(HttpStatus.OK)
    public OperationResponse calculate(Authentication authentication, @RequestBody OperationRequest operation) {
        try {
            String userName = authentication.getName();
            User user = userRepository.findByUsername(userName)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return operationService.executeOperation(operation, user);
        } catch (RuntimeException ex) {
            System.out.print(ex.getMessage());
            return null;
        }
    }

}
