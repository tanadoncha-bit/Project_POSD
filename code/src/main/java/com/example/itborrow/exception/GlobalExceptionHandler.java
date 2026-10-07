package com.example.itborrow.exception;

import com.example.itborrow.dto.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.security.core.AuthenticationException;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isHtmlRequest(HttpServletRequest req) {

        String path = req.getRequestURI()
                .substring(req.getContextPath().length());

        String accept = req.getHeader("Accept");

        return !path.startsWith("/api/")
                && accept != null
                && accept.contains("text/html");
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest req) {

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView("error/404");

            mav.addObject("status", 404);
            mav.addObject("error", "Resource Not Found");
            mav.addObject("message", ex.getMessage());

            mav.setStatus(HttpStatus.NOT_FOUND);

            return mav;
        }

        return buildResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                req);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public Object handleMissingResource(
            HttpServletRequest req) {

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView("error/404");

            mav.addObject("status", 404);
            mav.addObject("error", "Page Not Found");
            mav.addObject(
                    "message",
                    "ไม่พบหน้าที่คุณต้องการ");

            mav.setStatus(HttpStatus.NOT_FOUND);

            return mav;
        }

        return buildResponse(
                HttpStatus.NOT_FOUND,
                "The requested resource was not found.",
                req);
    }

    @ExceptionHandler(InvalidBorrowStateException.class)
    public Object handleInvalidState(
            InvalidBorrowStateException ex,
            HttpServletRequest req) {

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView("error/409");

            mav.addObject("status", 409);
            mav.addObject(
                    "error",
                    "Invalid Borrow State");
            mav.addObject(
                    "message",
                    ex.getMessage());

            mav.setStatus(HttpStatus.CONFLICT);

            return mav;
        }

        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                req);
    }

    @ExceptionHandler(EquipmentNotAvailableException.class)
    public Object handleEquipmentNotAvailable(
            EquipmentNotAvailableException ex,
            HttpServletRequest req) {

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView(
                    "error/409");

            mav.addObject("status", 409);
            mav.addObject(
                    "error",
                    "Equipment Not Available");
            mav.addObject(
                    "message",
                    ex.getMessage());

            mav.setStatus(HttpStatus.CONFLICT);

            return mav;
        }

        return buildResponse(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest req) {

        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> ((FieldError) err).getField()
                        + ": "
                        + err.getDefaultMessage())
                .collect(Collectors.toList());

        // Web → HTML
        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView(
                    "error/400");

            mav.addObject("status", 400);
            mav.addObject(
                    "error",
                    "Validation Error");
            mav.addObject(
                    "message",
                    "ข้อมูลที่ส่งมาไม่ถูกต้อง");
            mav.addObject(
                    "details",
                    details);

            mav.setStatus(HttpStatus.BAD_REQUEST);

            return mav;
        }

        // API → JSON
        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "ข้อมูลที่ส่งมาไม่ถูกต้อง",
                req.getRequestURI());

        body.setDetails(details);

        return ResponseEntity
                .badRequest()
                .body(body);
    }

    @ExceptionHandler(Exception.class)
    public Object handleUnexpected(
            Exception ex,
            HttpServletRequest req) {

        String id = java.util.UUID.randomUUID().toString();

        var origin = java.util.Arrays.stream(ex.getStackTrace())
                .filter(frame -> frame.getClassName()
                        .startsWith(
                                "com.example.itborrow."))
                .findFirst()
                .map(Object::toString)
                .orElse("framework");

        log.error(
                "Request {} failed: {} at {}",
                id,
                ex.getClass().getName(),
                origin);

        // Web → HTML
        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView(
                    "error/500");

            mav.addObject("status", 500);
            mav.addObject(
                    "error",
                    "Internal Server Error");
            mav.addObject(
                    "message",
                    "เกิดข้อผิดพลาดที่ไม่คาดคิดในระบบ กรุณาลองใหม่อีกครั้ง (Ref: "
                            + id
                            + ")");

            mav.setStatus(
                    HttpStatus.INTERNAL_SERVER_ERROR);

            return mav;
        }

        // API → JSON
        return ResponseEntity
                .status(
                        HttpStatus.INTERNAL_SERVER_ERROR)
                .header("X-Request-ID", id)
                .body(
                        new ErrorResponseDto(
                                500,
                                "Internal Server Error",
                                "An unexpected error occurred. Reference: "
                                        + id,
                                req.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Object denied(
            Exception ex,
            HttpServletRequest req) {

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView(
                    "error/403");

            mav.addObject("status", 403);
            mav.addObject(
                    "error",
                    "Access Denied");
            mav.addObject(
                    "message",
                    "คุณไม่มีสิทธิ์เข้าถึงข้อมูลในส่วนนี้");

            mav.setStatus(
                    HttpStatus.FORBIDDEN);

            return mav;
        }

        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Access denied",
                req);
    }

    @ExceptionHandler(com.example.itborrow.service.storage.StorageException.class)
    public Object storageUnavailable(
            Exception ex,
            HttpServletRequest req) {

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView(
                    "error/503");

            mav.addObject("status", 503);
            mav.addObject(
                    "error",
                    "Storage Service Unavailable");
            mav.addObject(
                    "message",
                    ex.getMessage());

            mav.setStatus(
                    HttpStatus.SERVICE_UNAVAILABLE);

            return mav;
        }

        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                ex.getMessage(),
                req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object invalidInput(
            IllegalArgumentException ex,
            HttpServletRequest req) {

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView(
                    "error/400");

            mav.addObject("status", 400);
            mav.addObject(
                    "error",
                    "Invalid Input");
            mav.addObject(
                    "message",
                    ex.getMessage());

            mav.setStatus(
                    HttpStatus.BAD_REQUEST);

            return mav;
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                req);
    }


    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Object badRequest(
            Exception ex,
            HttpServletRequest req) {

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView(
                    "error/400");

            mav.addObject("status", 400);
            mav.addObject(
                    "error",
                    "Invalid Request");
            mav.addObject(
                    "message",
                    "ข้อมูลที่ส่งมาไม่ถูกต้อง");

            mav.setStatus(
                    HttpStatus.BAD_REQUEST);

            return mav;
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid request data",
                req);
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public Object dataIntegrityConflict(
            org.springframework.dao.DataIntegrityViolationException ex,
            HttpServletRequest req) {

        String message = "Data conflicts with an existing record. Refresh and retry.";

        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {

            if (cause.getMessage() != null
                    && cause.getMessage()
                            .toLowerCase(
                                    java.util.Locale.ROOT)
                            .contains(
                                    "uq_equipment_storage_slot")) {

                message = "This storage slot is already assigned to another asset. Choose a different slot.";

                break;
            }
        }

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView(
                    "error/409");

            mav.addObject("status", 409);
            mav.addObject(
                    "error",
                    "Data Conflict");
            mav.addObject(
                    "message",
                    message);

            mav.setStatus(
                    HttpStatus.CONFLICT);

            return mav;
        }

        return buildResponse(
                HttpStatus.CONFLICT,
                message,
                req);
    }

    @ExceptionHandler(org.springframework.dao.PessimisticLockingFailureException.class)
    public Object lockingConflict(
            org.springframework.dao.PessimisticLockingFailureException ex,
            HttpServletRequest req) {

        String message = "The requested data is currently being used by another process. Please try again.";

        if (isHtmlRequest(req)) {

            ModelAndView mav = new ModelAndView(
                    "error/409");

            mav.addObject("status", 409);
            mav.addObject(
                    "error",
                    "Resource Locked");
            mav.addObject(
                    "message",
                    message);

            mav.setStatus(
                    HttpStatus.CONFLICT);

            return mav;
        }

        return buildResponse(
                HttpStatus.CONFLICT,
                message,
                req);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public Object uploadTooLarge(
            HttpServletRequest request) {

        String message = "Choose a JPG or PNG image up to 2 MB.";

        // API → JSON
        if (request.getRequestURI()
                .startsWith(
                        request.getContextPath()
                                + "/api/")) {

            return buildResponse(
                    HttpStatus.PAYLOAD_TOO_LARGE,
                    message,
                    request);
        }

        // Web → HTML
        ModelAndView mav = new ModelAndView(
                "error/413");

        mav.addObject("status", 413);
        mav.addObject(
                "error",
                "File Too Large");
        mav.addObject(
                "message",
                message);

        mav.setStatus(
                HttpStatus.PAYLOAD_TOO_LARGE);

        return mav;
    }

    @ExceptionHandler(AuthenticationException.class)
    public Object handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {

        // Web → HTML
        if (isHtmlRequest(request)) {
            ModelAndView mav = new ModelAndView("error/401");

            mav.addObject("status", 401);
            mav.addObject("error", "Unauthorized");
            mav.addObject(
                    "message",
                    "กรุณาเข้าสู่ระบบก่อนใช้งาน"
            );

            mav.setStatus(HttpStatus.UNAUTHORIZED);

            return mav;
        }

        // API → JSON
        return new ResponseEntity<>(
                new ErrorResponseDto(
                        HttpStatus.UNAUTHORIZED.value(),
                        "Unauthorized",
                        "กรุณาเข้าสู่ระบบก่อนใช้งาน",
                        request.getRequestURI()
                ),
                HttpStatus.UNAUTHORIZED
        );
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest req) {

        ErrorResponseDto body = new ErrorResponseDto(
                status.value(),
                status.getReasonPhrase(),
                message,
                req.getRequestURI());

        return ResponseEntity
                .status(status)
                .body(body);
    }
}