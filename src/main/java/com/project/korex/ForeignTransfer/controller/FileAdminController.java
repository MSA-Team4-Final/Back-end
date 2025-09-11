package com.project.korex.ForeignTransfer.controller;

import com.project.korex.ForeignTransfer.entity.FileUpload;
import com.project.korex.ForeignTransfer.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/admin/files")
@RequiredArgsConstructor
public class FileAdminController {

    private final FileUploadRepository fileUploadRepository;
    private final String uploadDir = "uploads/";

    // 파일 다운로드
    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) {
        FileUpload fileUpload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));
        return getResourceResponse(fileUpload, true);
    }

    // 파일 미리보기 (브라우저에서 바로 열림)
    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> previewFile(@PathVariable Long fileId) {
        FileUpload fileUpload = fileUploadRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        return getResourceResponse(fileUpload, false);
    }

    // 공통 처리
    private ResponseEntity<Resource> getResourceResponse(FileUpload fileUpload, boolean download) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(fileUpload.getStoredFilename()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                throw new RuntimeException("파일이 서버에 존재하지 않습니다.");
            }

            HttpHeaders headers = new HttpHeaders();
            if (download) {
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileUpload.getOriginalFilename() + "\"");
            }

            // Content-Type 설정 (이미지면 브라우저에서 보여짐)
            MediaType contentType = MediaType.parseMediaType(fileUpload.getContentType());
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(contentType)
                    .body(resource);

        } catch (MalformedURLException e) {
            throw new RuntimeException("파일 경로가 잘못되었습니다: " + e.getMessage());
        }
    }
}
