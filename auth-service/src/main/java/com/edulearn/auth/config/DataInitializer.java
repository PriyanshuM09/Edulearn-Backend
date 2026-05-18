package com.edulearn.auth.config;

import com.edulearn.auth.entity.User;
import com.edulearn.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String adminEmail = "captainnjackk9898@gmail.com";
        
        log.info("Ensuring master admin exists: {}", adminEmail);

        // 1. Find or create the master admin
        Optional<User> masterAdminOpt = userRepository.findByEmail(adminEmail);
        if (masterAdminOpt.isEmpty()) {
            User admin = new User();
            admin.setFullName("Master Admin");
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode("captain12")); // User requested password
            admin.setRole("ADMIN");
            admin.setApproved(true);
            userRepository.save(admin);
            log.info("Master admin created with password: captain12");
        } else {
            User admin = masterAdminOpt.get();
            admin.setPasswordHash(passwordEncoder.encode("captain12")); // Update to requested password
            admin.setRole("ADMIN");
            admin.setApproved(true); // Always ensure admin is approved
            userRepository.save(admin);
            log.info("Master admin credentials updated for {}", adminEmail);
        }

        // 2. Remove all other admins
        List<User> allAdmins = userRepository.findAllByRole("ADMIN");
        for (User admin : allAdmins) {
            if (!admin.getEmail().equalsIgnoreCase(adminEmail)) {
                log.info("Removing unauthorized admin record: {}", admin.getEmail());
                userRepository.delete(admin);
            }
        }
    }
}
