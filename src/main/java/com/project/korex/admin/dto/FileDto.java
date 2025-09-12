package com.project.korex.admin.dto;

import com.project.korex.ForeignTransfer.entity.FileUpload;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FileDto {
    private String name; // 원본 이름
    private String storedFileName; // 서버에 저장된 UUID 파일명
    private String url; // 다운로드 URL

}

