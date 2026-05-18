package com.edulearn.auth.config;

import com.edulearn.auth.entity.User;
import com.edulearn.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    @DisplayName("Run - Create Master Admin if missing")
    void run_CreateAdmin() throws Exception {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findAllByRole("ADMIN")).thenReturn(Collections.emptyList());

        dataInitializer.run();

        verify(userRepository, atLeastOnce()).save(any(User.class));
    }

    @Test
    @DisplayName("Run - Update Master Admin if exists")
    void run_UpdateAdmin() throws Exception {
        User admin = new User();
        admin.setEmail("captainnjackk9898@gmail.com");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(admin));
        
        User unauthorizedAdmin = new User();
        unauthorizedAdmin.setEmail("other@test.com");
        when(userRepository.findAllByRole("ADMIN")).thenReturn(List.of(admin, unauthorizedAdmin));

        dataInitializer.run();

        verify(userRepository, atLeastOnce()).save(admin);
        verify(userRepository).delete(unauthorizedAdmin);
    }
}
