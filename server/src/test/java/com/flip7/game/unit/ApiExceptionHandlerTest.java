package com.flip7.game.unit;

import com.flip7.game.exception.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiExceptionHandlerTest {

    @Test
    void handleValidation_returnsFirstValidationMessage() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(
                new ObjectError("dto", "Primer error"),
                new ObjectError("dto", "Segundo error")
        ));

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Primer error");
    }

    @Test
    void handleValidation_usesDefaultMessageWhenThereAreNoErrors() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of());

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Solicitud inválida");
    }

    @Test
    void handleValidation_usesDefaultMessageWhenFirstErrorMessageIsNull() {
        ApiExceptionHandler handler = new ApiExceptionHandler();
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(new ObjectError("dto", null)));

        ResponseEntity<Map<String, String>> response = handler.handleValidation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Solicitud inválida");
    }

    @Test
    void handleIllegalArgument_returnsConflictForKnownConflictMessages() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("Ya existe un jugador con ese nombre")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleIllegalArgument_returnsBadRequestForRegularMessages() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("dato inválido")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleIllegalArgument_returnsConflictForUnavailableRoomPhraseWithAccent() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleIllegalArgument(
                new IllegalArgumentException("La sala ya no está disponible para unirse")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleRuntime_returnsNotFoundForNotFoundMessages() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleRuntime(
                new RuntimeException("Partida no encontrada")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void handleRuntime_returnsBadRequestForOtherRuntimeMessages() {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        ResponseEntity<Map<String, String>> response = handler.handleRuntime(
                new RuntimeException("estado inválido")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void privateMessageDetectors_coverNullAndAlternativePhrases() throws Exception {
        ApiExceptionHandler handler = new ApiExceptionHandler();

        assertThat((Boolean) invoke(handler, "isNotFoundMessage", new Class[]{String.class}, (Object) null)).isFalse();
        assertThat((Boolean) invoke(handler, "isNotFoundMessage", new Class[]{String.class}, "jugador no encontrado")).isTrue();
        assertThat((Boolean) invoke(handler, "isNotFoundMessage", new Class[]{String.class}, "otro error")).isFalse();

        assertThat((Boolean) invoke(handler, "isConflictMessage", new Class[]{String.class}, (Object) null)).isFalse();
        assertThat((Boolean) invoke(handler, "isConflictMessage", new Class[]{String.class}, "La sala ya no esta disponible")).isTrue();
        assertThat((Boolean) invoke(handler, "isConflictMessage", new Class[]{String.class}, "otro error")).isFalse();
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
