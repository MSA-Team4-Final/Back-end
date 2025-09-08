package com.project.korex.auth.controller;

import com.project.korex.auth.dto.request.ImageParsingRequest;
import com.project.korex.auth.dto.response.OcrData;
import com.project.korex.auth.service.ClovaOcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ocr")
@AllArgsConstructor
public class ClovaOcrController {

    private final ClovaOcrService clovaOcrService;
    private final Path uploadDir = Paths.get("uploads");

    @Operation(summary = "OCR 이미지 업로드 및 분석")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "500", description = "서버 오류")
    })
    @PostMapping(value = "/upload-analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAndAnalyze(
            @Parameter(description = "OCR 분석할 이미지 파일", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            // 파일을 Base64로 변환
            String base64Image = Base64.getEncoder().encodeToString(file.getBytes());

            // OCR 실행
            ImageParsingRequest request = new ImageParsingRequest(null, base64Image); // URL 대신 Base64 사용
            String ocrResult = ClovaOcrService.execute(request);
            OcrData data = clovaOcrService.parseOcrResult(ocrResult);

            // 결과 반환
            Map<String, String> response = new HashMap<>();
            response.put("ocrResult", ocrResult);
            response.put("name", data.getName());
            response.put("birth", data.getBirth());
            response.put("rrn", data.getRrn());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // 업로드된 파일 브라우저 접근
    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) throws IOException {
        Path filePath = uploadDir.resolve(filename);
        Resource resource = new UrlResource(filePath.toUri());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, Files.probeContentType(filePath))
                .body(resource);
    }
}
