package com.project.korex.ForeignTransfer.repository;

import com.project.korex.ForeignTransfer.entity.FileUpload;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileUploadRepository extends JpaRepository<FileUpload, Long> {
}
