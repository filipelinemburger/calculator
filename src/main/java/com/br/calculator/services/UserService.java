package com.br.calculator.services;

import static com.br.calculator.enums.UserStatusEnum.ACTIVE;
import com.br.calculator.entities.User;
import com.br.calculator.exceptions.UserException;
import com.br.calculator.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private static final String INVALID_CREDENTIALS = "Invalid credentials";
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void createNewUser(User user) {
        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent()) {
            throw new UserException("User already exists");
        }
        validateUsername(user.getUsername());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setStatus(ACTIVE);
        userRepository.save(user);
    }

    private void validateUsername(String username) {
        // Check if username is empty
        if (username == null || username.trim().isEmpty()) {
            throw new UserException("Username cannot be empty.");
        }

        // Check the length of the username (e.g., between 3 and 20 characters)
        if (username.length() < 3 || username.length() > 20) {
            throw new UserException("Username must be between 3 and 20 characters long.");
        }

        // Check for allowed characters (e.g., alphanumeric and underscores only)
        String regex = "^[a-zA-Z0-9_]+$";
        if (!username.matches(regex)) {
            throw new UserException("Username can only contain letters, numbers, and underscores.");
        }

        // Custom business rule: no consecutive underscores
        if (username.contains("__")) {
            throw new UserException("Username cannot contain consecutive underscores.");
        }
        // If all checks pass, the username is valid
    }


    public User doLogin(User user) {
        User foundUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new UserException(INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
            throw new UserException(INVALID_CREDENTIALS);
        }
        return foundUser;
    }

}
