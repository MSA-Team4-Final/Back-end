package com.project.korex.ForeignTransfer.repository;

import com.project.korex.ForeignTransfer.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
    List<FileUpload> findByForeignTransferTransaction_Id(Long transferId);
}
