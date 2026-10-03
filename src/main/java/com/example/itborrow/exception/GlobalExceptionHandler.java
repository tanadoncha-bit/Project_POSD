package com.example.itborrow.exception;

import com.example.itborrow.dto.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ดักจับ exception ทุกตัวในระบบไว้ที่จุดเดียว แปลงเป็น ErrorResponseDto ที่ format เดียวกันหมด
 * ตรงตามข้อกำหนดข้อ 7: "Global Exception Handler (@RestControllerAdvice) + Error Response Format มาตรฐาน"
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ทรัพยากรไม่พบ -> 404
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    // การเปลี่ยนสถานะไม่ถูกต้อง (State Pattern throw) -> 409
    @ExceptionHandler(InvalidBorrowStateException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidState(InvalidBorrowStateException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    // อุปกรณ์ไม่พร้อมให้ยืม -> 409
    @ExceptionHandler(EquipmentNotAvailableException.class)
    public ResponseEntity<ErrorResponseDto> handleEquipmentNotAvailable(EquipmentNotAvailableException ex,
                                                                         HttpServletRequest req) {
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    // @Valid ใน Controller ล้มเหลว (Bean Validation) -> 400 พร้อมรายละเอียดทุก field ที่ผิด
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> ((FieldError) err).getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponseDto body = new ErrorResponseDto(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "ข้อมูลที่ส่งมาไม่ถูกต้อง",
                req.getRequestURI());
        body.setDetails(details);
        return ResponseEntity.badRequest().body(body);
    }

    // ทุก exception ที่ไม่ได้ดักไว้เฉพาะเจาะจง -> 500 (กันไม่ให้ stack trace หลุดออกไปหา client)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", req);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> denied(Exception ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", req);
    }
    @ExceptionHandler(com.example.itborrow.service.avatar.StorageException.class)
    public ResponseEntity<ErrorResponseDto> storageUnavailable(Exception ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), req);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> invalidInput(IllegalArgumentException ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> badRequest(Exception ex, HttpServletRequest req) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request data", req);
    }
    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class, org.springframework.dao.PessimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponseDto> conflict(Exception ex, HttpServletRequest req) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().toLowerCase(java.util.Locale.ROOT).contains("uq_equipment_storage_slot"))
                return buildResponse(HttpStatus.CONFLICT, "This storage slot is already assigned to another asset. Choose a different slot.", req);
        }
        return buildResponse(HttpStatus.CONFLICT, "Data conflicts with an existing record. Refresh and retry.", req);
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public org.springframework.web.servlet.ModelAndView uploadTooLarge() {
        return new org.springframework.web.servlet.ModelAndView("redirect:/profile?uploadError=size");
    }

    private ResponseEntity<ErrorResponseDto> buildResponse(HttpStatus status, String message, HttpServletRequest req) {
        ErrorResponseDto body = new ErrorResponseDto(
                status.value(), status.getReasonPhrase(), message, req.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }
}