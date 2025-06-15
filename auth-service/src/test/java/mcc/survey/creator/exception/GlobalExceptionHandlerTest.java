package mcc.survey.creator.exception;

import mcc.survey.creator.dto.ErrorResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.dao.DataIntegrityViolationException;


import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

public class GlobalExceptionHandlerTest {

    @Mock
    private WebRequest webRequest;

    @Mock
    private BindingResult bindingResult;

    // Cannot directly mock MethodParameter like this, it's a final class.
    // MethodParameter methodParameter = new MethodParameter(this.getClass().getDeclaredMethods()[0], -1);


    @InjectMocks
    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(webRequest.getDescription(false)).thenReturn("uri=/test/path");
        // Simulate dev environment for debug messages by setting the private field
        // This is tricky with private fields. For real tests, consider making 'environment' package-private
        // or having a setter, or using reflection (though less ideal for unit tests).
        // For this subtask, we'll assume 'isDevelopment()' might be true or test both paths if possible.
        // Alternatively, we can't easily test the debugMessage part here without Spring context.
    }

    @Test
    void handleResourceNotFoundException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Test resource not found");
        ResponseEntity<ErrorResponseDto> responseEntity = globalExceptionHandler.handleResourceNotFoundException(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(HttpStatus.NOT_FOUND.value(), responseEntity.getBody().getStatus());
        assertEquals("Test resource not found", responseEntity.getBody().getMessage());
        assertEquals("/test/path", responseEntity.getBody().getPath());
    }

    @Test
    void handleDuplicateResourceException() {
        DuplicateResourceException ex = new DuplicateResourceException("Test duplicate resource");
        ResponseEntity<ErrorResponseDto> responseEntity = globalExceptionHandler.handleDuplicateResourceException(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), responseEntity.getBody().getStatus());
        assertEquals("Test duplicate resource", responseEntity.getBody().getMessage());
    }

    @Test
    void handleMethodArgumentNotValid() {
        // We need a real MethodParameter for the MethodArgumentNotValidException constructor.
        // This is hard to mock correctly without a deeper setup.
        // For now, let's use a simpler approach for the exception itself.
        MethodParameter mockMethodParameter;
        try {
            // Attempt to get a real MethodParameter instance for a dummy method
            mockMethodParameter = new MethodParameter(this.getClass().getDeclaredMethod("dummyMethodForTest", String.class), 0);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e); // Should not happen
        }

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mockMethodParameter, bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(new FieldError("objectName", "field", "default message")));

        ResponseEntity<ErrorResponseDto> responseEntity = globalExceptionHandler.handleMethodArgumentNotValid(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getBody().getStatus());
        assertTrue(responseEntity.getBody().getMessage().contains("field: default message"));
    }

    // Dummy method for MethodParameter creation in tests
    public void dummyMethodForTest(String arg) {}


    @Test
    void handleAuthenticationException() {
        AuthenticationException ex = new AuthenticationCredentialsNotFoundException("Test auth error");
        ResponseEntity<ErrorResponseDto> responseEntity = globalExceptionHandler.handleAuthenticationException(ex, webRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(HttpStatus.UNAUTHORIZED.value(), responseEntity.getBody().getStatus());
        assertTrue(responseEntity.getBody().getMessage().contains("Test auth error"));
    }

    @Test
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Test access denied");
        ResponseEntity<ErrorResponseDto> responseEntity = globalExceptionHandler.handleAccessDeniedException(ex, webRequest);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(HttpStatus.FORBIDDEN.value(), responseEntity.getBody().getStatus());
        assertTrue(responseEntity.getBody().getMessage().contains("Test access denied"));
    }

    @Test
    void handleDataIntegrityViolationException() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Test data integrity error");
        ResponseEntity<ErrorResponseDto> responseEntity = globalExceptionHandler.handleDataIntegrityViolationException(ex, webRequest);

        assertEquals(HttpStatus.CONFLICT, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(HttpStatus.CONFLICT.value(), responseEntity.getBody().getStatus());
        assertTrue(responseEntity.getBody().getMessage().contains("Data integrity violation"));
    }

    @Test
    void handleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Test illegal argument");
        ResponseEntity<ErrorResponseDto> responseEntity = globalExceptionHandler.handleIllegalArgumentException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(HttpStatus.BAD_REQUEST.value(), responseEntity.getBody().getStatus());
        assertEquals("Test illegal argument", responseEntity.getBody().getMessage());
    }

    @Test
    void handleGlobalException() {
        Exception ex = new Exception("Test generic error");
        ResponseEntity<ErrorResponseDto> responseEntity = globalExceptionHandler.handleGlobalException(ex, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
        assertNotNull(responseEntity.getBody());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), responseEntity.getBody().getStatus());
        assertEquals("An unexpected error occurred. Please try again later.", responseEntity.getBody().getMessage());
    }
}
