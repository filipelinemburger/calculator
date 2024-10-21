package com.br.calculator.controllers;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.br.calculator.dto.OperationRequest;
import com.br.calculator.dto.OperationResponse;
import com.br.calculator.dto.RecordResponse;
import com.br.calculator.dto.UserStatsResponse;
import com.br.calculator.entities.User;
import com.br.calculator.exceptions.OperationException;
import com.br.calculator.repositories.UserRepository;
import com.br.calculator.services.OperationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class OperationControllerTest {

    @InjectMocks
    private OperationController operationController;

    @Mock
    private OperationService operationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Authentication authentication;

    private User mockUser;
    private String mockUsername;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockUsername = "testUser";
        mockUser = new User();
        mockUser.setUsername(mockUsername);
    }

    @Test
    void testGetOperations_Success() {
        // Mock authentication and userRepository behavior
        when(authentication.getName()).thenReturn(mockUsername);
        when(userRepository.findByUsername(mockUsername)).thenReturn(Optional.of(mockUser));

        // Mock operationService behavior
        List<RecordResponse> records = new ArrayList<>();
        when(operationService.getUserRecords(mockUser)).thenReturn(records);

        // Call the controller method
        ResponseEntity<List<RecordResponse>> response = operationController.getOperations(authentication);

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());

        // Verify that the mock methods were called
        verify(userRepository, times(1)).findByUsername(mockUsername);
        verify(operationService, times(1)).getUserRecords(mockUser);
    }

    @Test
    void testGetOperations_UserNotFound() {
        // Mock userRepository to return empty
        when(authentication.getName()).thenReturn(mockUsername);
        when(userRepository.findByUsername(mockUsername)).thenReturn(Optional.empty());

        // Call the controller method
        ResponseEntity<List<RecordResponse>> response = operationController.getOperations(authentication);

        // Assertions
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().size());

        // Verify that the mock methods were called
        verify(userRepository, times(1)).findByUsername(mockUsername);
        verify(operationService, times(0)).getUserRecords(mockUser); // Should not call the service
    }

    @Test
    void testGetUserStats_Success() {
        // Mock authentication and userRepository behavior
        when(authentication.getName()).thenReturn(mockUsername);
        when(userRepository.findByUsername(mockUsername)).thenReturn(Optional.of(mockUser));

        // Mock operationService behavior
        UserStatsResponse statsResponse = new UserStatsResponse();
        when(operationService.getUserStats(mockUser)).thenReturn(statsResponse);

        // Call the controller method
        ResponseEntity<UserStatsResponse> response = operationController.getUserStats(authentication);

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());

        // Verify that the mock methods were called
        verify(userRepository, times(1)).findByUsername(mockUsername);
        verify(operationService, times(1)).getUserStats(mockUser);
    }

    @Test
    void testGetUserStats_UserNotFound() {
        // Mock userRepository to return empty
        when(authentication.getName()).thenReturn(mockUsername);
        when(userRepository.findByUsername(mockUsername)).thenReturn(Optional.empty());

        // Call the controller method
        ResponseEntity<UserStatsResponse> response = operationController.getUserStats(authentication);

        // Assertions
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody());

        // Verify that the mock methods were called
        verify(userRepository, times(1)).findByUsername(mockUsername);
        verify(operationService, times(0)).getUserStats(mockUser); // Should not call the service
    }

    @Test
    void testCalculate_Success() {
        // Mock authentication and userRepository behavior
        when(authentication.getName()).thenReturn(mockUsername);
        when(userRepository.findByUsername(mockUsername)).thenReturn(Optional.of(mockUser));

        // Mock operationService behavior
        OperationRequest request = new OperationRequest();
        OperationResponse responseMock = new OperationResponse();
        when(operationService.executeOperation(request, mockUser)).thenReturn(responseMock);

        // Call the controller method
        ResponseEntity<?> response = operationController.calculate(authentication, request);

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(responseMock, response.getBody());

        // Verify that the mock methods were called
        verify(userRepository, times(1)).findByUsername(mockUsername);
        verify(operationService, times(1)).executeOperation(request, mockUser);
    }

    @Test
    void testCalculate_OperationException() {
        // Mock authentication and userRepository behavior
        when(authentication.getName()).thenReturn(mockUsername);
        when(userRepository.findByUsername(mockUsername)).thenReturn(Optional.of(mockUser));

        // Mock operationService behavior to throw exception
        OperationRequest request = new OperationRequest();
        when(operationService.executeOperation(request, mockUser)).thenThrow(new OperationException("Operation failed"));

        // Call the controller method
        ResponseEntity<?> response = operationController.calculate(authentication, request);

        // Assertions
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Operation failed", response.getBody());

        // Verify that the mock methods were called
        verify(userRepository, times(1)).findByUsername(mockUsername);
        verify(operationService, times(1)).executeOperation(request, mockUser);
    }
}

