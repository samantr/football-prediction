package com.example.footballprediction.config;

import com.example.footballprediction.domain.Role;
import com.example.footballprediction.domain.User;
import com.example.footballprediction.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean createAdmin;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminDisplayName;

    public AdminUserInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.create-admin:true}") boolean createAdmin,
            @Value("${app.bootstrap.admin-email:admin@example.com}") String adminEmail,
            @Value("${app.bootstrap.admin-password:admin123}") String adminPassword,
            @Value("${app.bootstrap.admin-display-name:Admin}") String adminDisplayName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.createAdmin = createAdmin;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.adminDisplayName = adminDisplayName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!createAdmin || userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = new User();
        admin.setEmail(adminEmail.trim().toLowerCase());
        admin.setDisplayName(adminDisplayName.trim());
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }
}
