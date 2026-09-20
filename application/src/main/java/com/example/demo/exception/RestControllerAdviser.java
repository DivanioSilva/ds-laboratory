package com.example.demo.exception;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestControllerAdviser {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        ProblemDetail problemDetail = createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Dados inválidos",
                "Um ou mais campos possuem valores inválidos",
                request);

        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors()
                .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }

    @ExceptionHandler(PersonNotFoundException.class)
    ProblemDetail handlePersonNotFound(
            PersonNotFoundException exception,
            HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Pessoa não encontrada",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(AddressNotFoundException.class)
    ProblemDetail handleAddressNotFound(
            AddressNotFoundException exception,
            HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.NOT_FOUND,
                "Endereço não encontrado",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.CONFLICT,
                "Conflito de dados",
                "A operação viola uma restrição da base de dados",
                request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.BAD_REQUEST,
                "Pedido inválido",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(PersonImportException.class)
    ProblemDetail handlePersonImport(
            PersonImportException exception,
            HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Falha na importação",
                exception.getMessage(),
                request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {
        return createProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Ocorreu um erro inesperado ao processar o pedido",
                request);
    }

    private ProblemDetail createProblemDetail(
            HttpStatus status,
            String title,
            String detail,
            HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setTitle(title);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        return problemDetail;
    }
}
