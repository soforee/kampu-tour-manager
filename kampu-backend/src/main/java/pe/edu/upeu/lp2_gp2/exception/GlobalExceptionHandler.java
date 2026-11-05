package pe.edu.upeu.lp2_gp2.exception;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import pe.edu.upeu.lp2_gp2.dto.ErrorDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorDTO> responder(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(new ErrorDTO(status.value(), mensaje));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorDTO> noEncontrado(ResourceNotFoundException e) {
        return responder(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorDTO> negocio(BusinessException e) {
        return responder(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorDTO> duplicado(DuplicateResourceException e) {
        return responder(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MailException.class)
    public ResponseEntity<ErrorDTO> correo(MailException e) {
        System.err.println("Error de correo: " + e.getMessage());
        return responder(HttpStatus.BAD_GATEWAY, "No se pudo enviar el correo. Revisa la configuración SMTP del servidor.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDTO> validacion(MethodArgumentNotValidException e) {
        List<ObjectError> errores = e.getBindingResult().getAllErrors();
        String mensaje = errores.isEmpty() ? "Datos inválidos." : errores.get(0).getDefaultMessage();
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorDTO> validacionMetodo(HandlerMethodValidationException e) {
        List<? extends MessageSourceResolvable> errores = e.getAllErrors();
        String mensaje = errores.isEmpty() ? "Datos inválidos." : errores.get(0).getDefaultMessage();
        return responder(HttpStatus.BAD_REQUEST, mensaje);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDTO> formato(HttpMessageNotReadableException e) {
        if (e.getCause() instanceof MismatchedInputException) {
            MismatchedInputException error = (MismatchedInputException) e.getCause();
            if (!error.getPath().isEmpty()) {
                String campo = error.getPath().get(error.getPath().size() - 1).getFieldName();
                if (campo != null) {
                    return responder(HttpStatus.BAD_REQUEST,
                            "El campo " + nombreCampo(campo) + " debe ser " + tipoEsperado(error.getTargetType()) + ".");
                }
            }
        }
        return responder(HttpStatus.BAD_REQUEST, "El formato de los datos enviados no es válido.");
    }

    private String nombreCampo(String campo) {
        if (campo.equals("dias")) return "días";
        if (campo.equals("precioBase")) return "precio base";
        if (campo.equals("cuposMaximos")) return "cupos máximos";
        if (campo.equals("fechaInicio")) return "fecha de inicio";
        if (campo.equals("fechaFin")) return "fecha fin";
        if (campo.equals("fechaNacimiento")) return "fecha de nacimiento";
        if (campo.equals("paqueteId")) return "paquete";
        if (campo.equals("clienteId")) return "cliente";
        if (campo.equals("salidaId")) return "salida";
        if (campo.equals("guiaId")) return "guía";
        if (campo.equals("transporteId")) return "transporte";
        if (campo.equals("reservaId")) return "reserva";
        return campo;
    }

    private String tipoEsperado(Class<?> tipo) {
        if (tipo == null) return "un valor válido";
        if (tipo == Integer.class || tipo == int.class || tipo == Long.class || tipo == long.class) return "un número entero";
        if (tipo == BigDecimal.class || tipo == Double.class || tipo == double.class) return "un número";
        if (tipo == LocalDate.class) return "una fecha válida (AAAA-MM-DD)";
        return "un valor válido";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDTO> parametro(MethodArgumentTypeMismatchException e) {
        return responder(HttpStatus.BAD_REQUEST, "El parámetro " + e.getName() + " no tiene un formato válido.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorDTO> archivoGrande(MaxUploadSizeExceededException e) {
        return responder(HttpStatus.PAYLOAD_TOO_LARGE, "La imagen supera el tamaño máximo permitido.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorDTO> integridad(DataIntegrityViolationException e) {
        System.err.println("Violación de integridad: " + e.getMostSpecificCause().getMessage());
        return responder(HttpStatus.CONFLICT, "La operación viola una restricción de la base de datos (registro duplicado o en uso).");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDTO> metodo(HttpRequestMethodNotSupportedException e) {
        return responder(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no soportado.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDTO> rutaInexistente(NoResourceFoundException e) {
        return responder(HttpStatus.NOT_FOUND, "Recurso no encontrado.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> general(Exception e) {
        System.err.println("Error no controlado: " + e.getMessage());
        e.printStackTrace();
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Ocurrió un error inesperado en el servidor.");
    }
}
