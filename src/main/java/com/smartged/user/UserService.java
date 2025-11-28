package com.smartged.user;

import com.smartged.auth.dto.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleRepository = roleRepository;
    }

    public UserEntity registerNewUser(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.username).isPresent() || userRepository.findByEmail(registerRequest.email).isPresent()) {
            throw new IllegalArgumentException("Username or email already exists");
        }
        
        UserEntity newUser = new UserEntity();
        newUser.setUsername(registerRequest.username);
        newUser.setEmail(registerRequest.email);
        newUser.setPasswordHash(passwordEncoder.encode(registerRequest.password));
        newUser.setEnabled(true); // Enable user by default

        // Assign a default role
        RoleEntity userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("Error: Role is not found."));
        newUser.setRoles(Collections.singleton(userRole));

        return userRepository.save(newUser);
    }
}
