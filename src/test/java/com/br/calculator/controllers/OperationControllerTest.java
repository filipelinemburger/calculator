package com.br.calculator.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.br.calculator.dto.RecordResponse;
import com.br.calculator.entities.User;
import com.br.calculator.repositories.UserRepository;
import com.br.calculator.services.OperationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
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

        // Create a mock pageable and a mock page
        Pageable pageable = PageRequest.of(0, 10);
        Page<RecordResponse> mockPage = new PageImpl<>(new ArrayList<>());

        // Mock operationService behavior
        when(operationService.getUserRecords(mockUser, pageable)).thenReturn(mockPage);

        // Call the controller method
        ResponseEntity<Page<RecordResponse>> response = operationController.getOperations(authentication, 0, 10);

        // Assertions
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(0, response.getBody().getContent().size());

        // Verify that the mock methods were called
        verify(userRepository, times(1)).findByUsername(mockUsername);
        verify(operationService, times(1)).getUserRecords(mockUser, pageable);
    }

    @Test
    void testGetOperations_UserNotFound() {
        // Mock userRepository to return empty
        when(authentication.getName()).thenReturn(mockUsername);
        when(userRepository.findByUsername(mockUsername)).thenReturn(Optional.empty());

        // Create a pageable object
        Pageable pageable = PageRequest.of(0, 10);

        // Call the controller method
        ResponseEntity<Page<RecordResponse>> response = operationController.getOperations(authentication, 0, 10);

        // Assertions
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody()); // Assert the body is null when user is not found

        // Verify that the mock methods were called
        verify(userRepository, times(1)).findByUsername(mockUsername);
        verify(operationService, times(0)).getUserRecords(any(User.class), eq(pageable)); // Service should not be called
    }


}
