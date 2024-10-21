package com.br.calculator.services;

import com.br.calculator.entities.User;
import com.br.calculator.exceptions.UserException;
import com.br.calculator.repositories.UserRepository;
import com.br.calculator.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static com.br.calculator.enums.UserStatusEnum.ACTIVE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user = new User();
        user.setUsername("test_user");
        user.setPassword("password123");
    }

    // Test: Successful creation of a new user
    @Test
    void testCreateNewUser_Success() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(user.getPassword())).thenReturn("encodedPassword");

        userService.createNewUser(user);

        assertEquals(ACTIVE, user.getStatus());
        verify(userRepository, times(1)).save(user);
    }

    // Test: User already exists
    @Test
    void testCreateNewUser_UserAlreadyExists() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        UserException exception = assertThrows(UserException.class, () -> userService.createNewUser(user));
        assertEquals("User already exists", exception.getMessage());
    }

    // Test: Username validation (empty username)
    @Test
    void testCreateNewUser_UsernameEmpty() {
        user.setUsername("");
        UserException exception = assertThrows(UserException.class, () -> userService.createNewUser(user));
        assertEquals("Username cannot be empty.", exception.getMessage());
    }

    // Test: Username validation (too short or too long)
    @Test
    void testCreateNewUser_UsernameInvalidLength() {
        user.setUsername("ab");  // too short
        UserException exception = assertThrows(UserException.class, () -> userService.createNewUser(user));
        assertEquals("Username must be between 3 and 20 characters long.", exception.getMessage());

        user.setUsername("a".repeat(21));  // too long
        exception = assertThrows(UserException.class, () -> userService.createNewUser(user));
        assertEquals("Username must be between 3 and 20 characters long.", exception.getMessage());
    }

    // Test: Username validation (invalid characters)
    @Test
    void testCreateNewUser_InvalidCharacters() {
        user.setUsername("test@user");  // Invalid character
        UserException exception = assertThrows(UserException.class, () -> userService.createNewUser(user));
        assertEquals("Username can only contain letters, numbers, and underscores.", exception.getMessage());
    }

    // Test: Username validation (consecutive underscores)
    @Test
    void testCreateNewUser_ConsecutiveUnderscores() {
        user.setUsername("test__user");  // Consecutive underscores
        UserException exception = assertThrows(UserException.class, () -> userService.createNewUser(user));
        assertEquals("Username cannot contain consecutive underscores.", exception.getMessage());
    }

    // Test: Successful login
    @Test
    void testDoLogin_Success() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        User loggedInUser = userService.doLogin(user);
        assertNotNull(loggedInUser);
    }

    // Test: Login with invalid credentials
    @Test
    void testDoLogin_InvalidCredentials() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());

        UserException exception = assertThrows(UserException.class, () -> userService.doLogin(user));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    // Test: Login with incorrect password
    @Test
    void testDoLogin_IncorrectPassword() {
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        UserException exception = assertThrows(UserException.class, () -> userService.doLogin(user));
        assertEquals("Invalid credentials", exception.getMessage());
    }
}
