package com.br.calculator.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.br.calculator.dto.OperationRequest;
import com.br.calculator.dto.OperationResponse;
import com.br.calculator.dto.RecordResponse;
import com.br.calculator.dto.UserStatsResponse;
import com.br.calculator.entities.User;
import com.br.calculator.exceptions.OperationException;
import com.br.calculator.exceptions.UserException;
import com.br.calculator.services.OperationService;
import com.br.calculator.services.UserService;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

class OperationControllerTest {

    @Mock
    private OperationService operationService;

    @Mock
    private UserService userService;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private OperationController operationController;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setUsername("testUser");
    }

    private void mockAuthentication() {
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("testUser");
        when(userService.findUserByUserName("testUser")).thenReturn(user);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetOperationsSuccess() {
        mockAuthentication();

        Pageable pageable = PageRequest.of(0, 10);
        Page<RecordResponse> mockRecords = new PageImpl<>(Collections.emptyList());
        when(operationService.getUserRecords(user, pageable)).thenReturn(mockRecords);

        ResponseEntity<Page<RecordResponse>> response = operationController.getOperations(0, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockRecords, response.getBody());
        verify(operationService, times(1)).getUserRecords(user, pageable);
    }

    @Test
    void testGetUserStatsSuccess() {
        mockAuthentication();

        UserStatsResponse mockStats = new UserStatsResponse();
        when(operationService.getUserStats(user)).thenReturn(mockStats);

        ResponseEntity<?> response = operationController.getUserStats();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockStats, response.getBody());
        verify(operationService, times(1)).getUserStats(user);
    }

    @Test
    void testGetUserStatsThrowsUserException() {
        mockAuthentication();

        when(operationService.getUserStats(user)).thenThrow(new UserException("User not found"));

        ResponseEntity<?> response = operationController.getUserStats();

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User not found", response.getBody());
        verify(operationService, times(1)).getUserStats(user);
    }

    @Test
    void testCalculateSuccess() {
        mockAuthentication();

        OperationRequest operationRequest = new OperationRequest();
        OperationResponse mockResponse = new OperationResponse();
        when(operationService.executeOperation(operationRequest, user)).thenReturn(mockResponse);

        ResponseEntity<?> response = operationController.calculate(operationRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockResponse, response.getBody());
        verify(operationService, times(1)).executeOperation(operationRequest, user);
    }

    @Test
    void testCalculateThrowsOperationException() {
        mockAuthentication();

        OperationRequest operationRequest = new OperationRequest();
        when(operationService.executeOperation(operationRequest, user)).thenThrow(new OperationException("Invalid operation"));

        ResponseEntity<?> response = operationController.calculate(operationRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid operation", response.getBody());
        verify(operationService, times(1)).executeOperation(operationRequest, user);
    }

    @Test
    void testCalculateThrowsUserException() {
        mockAuthentication();

        OperationRequest operationRequest = new OperationRequest();
        when(operationService.executeOperation(operationRequest, user)).thenThrow(new UserException("User not found"));

        ResponseEntity<?> response = operationController.calculate(operationRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("User not found", response.getBody());
        verify(operationService, times(1)).executeOperation(operationRequest, user);
    }
}
