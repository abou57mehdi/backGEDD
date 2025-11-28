package com.smartged.security;

import com.smartged.user.RoleEntity;
import com.smartged.user.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            RoleEntity userRole = new RoleEntity();
            userRole.setName("ROLE_USER");
            roleRepository.save(userRole);
        }
    }
}
