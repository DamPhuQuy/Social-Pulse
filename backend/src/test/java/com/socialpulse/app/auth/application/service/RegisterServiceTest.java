package com.socialpulse.app.auth.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.auth.application.usecase.OtpUseCase;
import com.socialpulse.app.user.application.dto.request.UserCreationRequest;
import com.socialpulse.app.user.application.dto.response.UserCreationResponse;
import com.socialpulse.app.user.application.usecase.CreateUserUseCase;

@ExtendWith(MockitoExtension.class)
class RegisterServiceTest {

    @Mock private CreateUserUseCase createUserUseCase;
    @Mock private OtpUseCase otpUseCase;

    private RegisterService service;

    @BeforeEach
    void setUp() {
        service = new RegisterService(createUserUseCase, otpUseCase);
    }

    @Test
    void register_createsUserAndSendsOtp() {
        UserCreationRequest request = UserCreationRequest.builder()
                .email("user@mail.com").username("user123").rawPassword("P@ss1234").confirmPassword("P@ss1234").build();
        UserCreationResponse response = UserCreationResponse.builder().id(1L).email("user@mail.com").build();
        when(createUserUseCase.createUser(request)).thenReturn(response);

        UserCreationResponse result = service.register(request);

        assertEquals(1L, result.getId());
        verify(otpUseCase).generateToStoreAndSendEmail("user@mail.com");
    }
}
