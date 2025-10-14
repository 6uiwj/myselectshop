package com.sparta.myselectshop.controller;

import com.sparta.myselectshop.dto.FolderRequestDto;
import com.sparta.myselectshop.dto.FolderResponseDto;
import com.sparta.myselectshop.exception.RestApiException;
import com.sparta.myselectshop.security.UserDetailsImpl;
import com.sparta.myselectshop.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    //폴더 생성 api
    //바디쪽에서 folder 이름이 넘어올 것임
    @PostMapping("/folders")
    public void addFolder(@RequestBody FolderRequestDto folderRequestDto, @AuthenticationPrincipal UserDetailsImpl userDetails){
        List<String> folderNames = folderRequestDto.getFolderNames();
        folderService.addFolders(folderNames, userDetails.getUser());

    }

    //회원이 등록한 모든 폴더 조회 -> 회원정보 필요
    @GetMapping("/folders")
    public List<FolderResponseDto> getFolders(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails){

        return folderService.getFolders(userDetails.getUser());
    }

    //@ExceptionHandler : 컨트롤러에서 발생한 예외를 잡음
    @ExceptionHandler({IllegalArgumentException.class}) //IllegalArgumentException이 터졌을 때 여기서 잡음
    public ResponseEntity<RestApiException> handleException(IllegalArgumentException ex) {
        System.out.println("FolderController.handleException");
        RestApiException restApiException = new RestApiException(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
        return new ResponseEntity<>(
                // HTTP body
                restApiException,
                // HTTP status code
                HttpStatus.BAD_REQUEST
        );
    }
}
