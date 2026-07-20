package com.kbinvestory.backend.global.error;

import com.kbinvestory.backend.core.exception.FieldError;

import java.util.List;

public record ErrorResponse(String code, String message, List<FieldError> fieldErrors) {}