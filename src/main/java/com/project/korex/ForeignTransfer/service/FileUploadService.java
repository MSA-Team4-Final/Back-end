package com.project.korex.ForeignTransfer.service;

import com.project.korex.ForeignTransfer.entity.FileUpload;
import com.project.korex.ForeignTransfer.entity.ForeignTransferTransaction;
import com.project.korex.ForeignTransfer.repository.FileUploadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileUploadService {

    private final FileUploadRepository fileUploadRepository;

    @Value("${file.upload-dir}")  // yml에서 경로 읽어오기
    private String uploadDir;

    /**
     * 단순 파일 업로드 (서버 저장 파일명 반환)
     */
    public String uploadFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        try {
            // 원본 파일 확장자
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }

            // 서버에 저장할 파일명
            String storedFilename = UUID.randomUUID() + extension;

            // 업로드 경로 확인 및 생성
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);  // 없으면 폴더 생성
            }

            // 실제 파일 저장
            Path filePath = uploadPath.resolve(storedFilename);
            file.transferTo(filePath.toFile());

            return storedFilename;

        } catch (IOException e) {
            throw new RuntimeException("파일 업로드 실패: " + e.getMessage(), e);
        }
    }

    /**
     * ForeignTransferTransaction에 연결 후 FileUpload 저장
     */
    public FileUpload uploadFileToTransaction(ForeignTransferTransaction transaction, MultipartFile file, String fileType) {
        if (file == null || file.isEmpty()) return null;

        String storedFilename = uploadFile(file);  // 실제 저장 파일명
        String url = "/uploads/" + storedFilename; // 웹 접근용 URL

        FileUpload fu = new FileUpload();
        fu.setForeignTransferTransaction(transaction);
        fu.setOriginalFilename(file.getOriginalFilename());
        fu.setStoredFilename(storedFilename);
        fu.setFileUrl(url);
        fu.setFileType(fileType);
        fu.setFileSize(file.getSize());
        fu.setContentType(file.getContentType());
        fu.setUploadedAt(LocalDateTime.now());

        return fileUploadRepository.save(fu);
    }
}
