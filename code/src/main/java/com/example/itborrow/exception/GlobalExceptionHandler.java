package com.example.itborrow.exception;

import com.example.itborrow.dto.response.ErrorResponseDto;
import com.example.itborrow.service.storage.StorageException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {

        if (wantsHtml(req)) {

            var page = new ModelAndView("error/404");
            page.setStatus(HttpStatus.NOT_FOUND);

            return page;
        }

        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleMissingResource(HttpServletRequest req) {

        String path = req.getRequestURI().substring(req.getContextPath().length());

        String accept = req.getHeader("Accept");

        if (!path.startsWith("/api/")
                && !path.startsWith("/css/")
                && !path.startsWith("/js/")
                && !path.startsWith("/images/")
                && accept != null
                && accept.contains("text/html")) {

            var page = new ModelAndView("error/404");
            page.setStatus(HttpStatus.NOT_FOUND);

            return page;
        }

        return buildResponse(HttpStatus.NOT_FOUND, "The requested resource was not found.", req);
    }

    @ExceptionHandler(InvalidBorrowStateException.class)
    public Object handleInvalidState(InvalidBorrowStateException ex, HttpServletRequest req) {

        return buildHtmlOrJson(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(EquipmentNotAvailableException.class)
    public Object handleEquipmentNotAvailable(
            EquipmentNotAvailableException ex, HttpServletRequest req) {

        return buildHtmlOrJson(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {

        if (wantsHtml(req)) {

            var page = new ModelAndView("error/400");
            page.setStatus(HttpStatus.BAD_REQUEST);

            return page;
        }

        List<String> details =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(err -> ((FieldError) err).getField() + ": " + err.getDefaultMessage())
                        .collect(Collectors.toList());

        ErrorResponseDto body =
                new ErrorResponseDto(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        "ข้อมูลที่ส่งมาไม่ถูกต้อง",
                        req.getRequestURI());

        body.setDetails(details);

        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object invalidInput(IllegalArgumentException ex, HttpServletRequest req) {

        return buildHtmlOrJson(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object badRequest(Exception ex, HttpServletRequest req) {

        return buildHtmlOrJson(HttpStatus.BAD_REQUEST, "Invalid request data", req);
    }

    @ExceptionHandler({
        TypeMismatchException.class,
        MissingServletRequestParameterException.class,
        HttpRequestMethodNotSupportedException.class,
        HttpMediaTypeNotSupportedException.class,
        HttpMediaTypeNotAcceptableException.class,
        HandlerMethodValidationException.class
    })
    public Object frameworkRequestError(
            Exception ex, HttpServletRequest req, HttpServletResponse response) {
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (ex instanceof ErrorResponse error) {
            status = HttpStatus.valueOf(error.getStatusCode().value());
            error.getHeaders()
                    .forEach(
                            (name, values) ->
                                    values.forEach(value -> response.addHeader(name, value)));
        }
        return buildHtmlOrJson(status, status.getReasonPhrase(), req);
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(Exception ex, HttpServletRequest req) {

        String id = UUID.randomUUID().toString();

        var origin =
                Arrays.stream(ex.getStackTrace())
                        .filter(frame -> frame.getClassName().startsWith("com.example.itborrow."))
                        .findFirst()
                        .map(Object::toString)
                        .orElse("framework");

        Throwable root = ex;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        log.error(
                "Request {} failed: {} at {}; root cause type: {}",
                id,
                ex.getClass().getName(),
                origin,
                root.getClass().getName());

        if (wantsHtml(req)) {

            var page = new ModelAndView("error/500");
            page.addObject("requestId", id);
            page.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);

            return page;
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-ID", id)
                .body(
                        new ErrorResponseDto(
                                500,
                                "Internal Server Error",
                                "An unexpected error occurred. Reference: " + id,
                                req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object denied(Exception ex, HttpServletRequest req) {

        return buildHtmlOrJson(HttpStatus.FORBIDDEN, "Access denied", req);
    }

    @ExceptionHandler(StorageException.class)
    public Object storageUnavailable(Exception ex, HttpServletRequest req) {

        return buildHtmlOrJson(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req);
    }

    @ExceptionHandler({
        DataIntegrityViolationException.class,
        PessimisticLockingFailureException.class
    })
    public Object conflict(Exception ex, HttpServletRequest req) {

        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {

            if (cause.getMessage() != null
                    && cause.getMessage()
                            .toLowerCase(Locale.ROOT)
                            .contains("uq_equipment_storage_slot")) {

                return buildHtmlOrJson(
                        HttpStatus.CONFLICT,
                        "This storage slot is already assigned to another asset. Choose a different"
                                + " slot.",
                        req);
            }
        }

        return buildHtmlOrJson(
                HttpStatus.CONFLICT,
                "Data conflicts with an existing record. Refresh and retry.",
                req);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object uploadTooLarge(HttpServletRequest request) {

        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {

            return buildResponse(
                    HttpStatus.PAYLOAD_TOO_LARGE, "Choose a JPG or PNG image up to 2 MB.", request);
        }

        return new ModelAndView("redirect:/profile?uploadError=size");
    }

    private boolean wantsHtml(HttpServletRequest req) {

        String path = req.getRequestURI().substring(req.getContextPath().length());

        String accept = req.getHeader("Accept");

        return !path.startsWith("/api/") && accept != null && accept.contains("text/html");
    }

    private Object buildHtmlOrJson(HttpStatus status, String message, HttpServletRequest req) {

        if (wantsHtml(req)) {

            String view =
                    Set.of(400, 401, 403, 404, 500, 503).contains(status.value())
                            ? "error/" + status.value()
                            : "error/generic";
            var page = new ModelAndView(view);
            page.addObject("statusCode", status.value());
            page.addObject("statusTitle", status.getReasonPhrase());
            page.addObject("errorMessage", message);

            page.setStatus(status);

            return page;
        }

        return buildResponse(status, message, req);
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(
            HttpStatus status, String message, HttpServletRequest req) {

        ErrorResponseDto body =
                new ErrorResponseDto(
                        status.value(), status.getReasonPhrase(), message, req.getRequestURI());

        return ResponseEntity.status(status).body(body);
    }
}
