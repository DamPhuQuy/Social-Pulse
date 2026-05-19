package com.socialpulse.app.discovery.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SearchUsersServiceTest {

    @Mock private UserRepository userRepository;

    private SearchUsersService service;

    @BeforeEach
    void setUp() {
        service = new SearchUsersService(userRepository);
    }

    @Test
    void searchUsers_returnsResults() {
        Page<User> page = new PageImpl<>(List.of(User.builder().id(1L).username("john").build()));
        when(userRepository.searchByQuery(eq("john"), any())).thenReturn(page);

        PageResponse<?> result = service.searchUsers("john", 0, 10);

        assertNotNull(result);
        assertFalse(result.getItems().isEmpty());
    }

    @Test
    void searchUsers_blankQuery_returnsEmpty() {
        PageResponse<?> result = service.searchUsers("  ", 0, 10);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        verifyNoInteractions(userRepository);
    }
}
