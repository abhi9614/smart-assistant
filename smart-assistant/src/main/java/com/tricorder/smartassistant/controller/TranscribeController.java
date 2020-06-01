package com.tricorder.smartassistant.controller;

import com.tricorder.smartassistant.constant.GeneralConstant;
import com.tricorder.smartassistant.dto.response.ApiResponse;
import com.tricorder.smartassistant.service.TranscribeServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(GeneralConstant.TRANSCRIBE)
public class TranscribeController {
    @Autowired
    private TranscribeServiceImpl transcribeService;

    @RequestMapping("/check")
    ResponseEntity<ApiResponse> check() {
        return ResponseEntity.ok().body(new ApiResponse(HttpStatus.OK, "running", transcribeService.check()));
    }
}
