package com.sa.event_mng.shared.application.service;

import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Service
public class FileService {

    @Value("${app.file.base-url}")
    private String fileBaseUrl;

    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
        
        File uploadDir = new File("uploads/");
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        try {
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            File destinationFile = new File(uploadDir.getAbsolutePath() + File.separator + filename);
            file.transferTo(destinationFile);
            return fileBaseUrl + "/" + filename;
        } catch (IOException e) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }
}
