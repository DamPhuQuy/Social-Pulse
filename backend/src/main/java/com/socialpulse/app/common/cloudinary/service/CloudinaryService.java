package com.socialpulse.app.common.cloudinary.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.SystemCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CloudinaryService {
    private static final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);
    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(SystemCode.UPLOAD_FAILED);
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap("resource_type", "auto")
            );

            Object secureUrl = result.get("secure_url");
            if (secureUrl == null) {
                logger.error("Cloudinary upload successful but secure_url is null. Result: {}", result);
                throw new AppException(SystemCode.UPLOAD_FAILED);
            }

            return secureUrl.toString();
        } catch (Exception e) {
            logger.error("Cloudinary upload failed: {}", e.getMessage(), e);
            throw new AppException(SystemCode.UPLOAD_FAILED);
        }
    }
}
