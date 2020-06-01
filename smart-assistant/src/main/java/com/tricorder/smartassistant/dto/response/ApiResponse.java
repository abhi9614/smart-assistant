package com.tricorder.smartassistant.dto.response;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class ApiResponse {

    private String status;
    private String message;
    private Object data;

    public ApiResponse(String status, String message, Object data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    public ApiResponse(HttpStatus status, String message, Object data) {
        this.status = status.getReasonPhrase();
        this.message = message;
        this.data = data;
    }

    public ApiResponse(Object data) {
        this.status = HttpStatus.OK.toString();
        this.message = HttpStatus.OK.getReasonPhrase();
        this.data = data;
    }
}