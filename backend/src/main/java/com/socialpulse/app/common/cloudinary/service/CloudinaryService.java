package com.socialpulse.app.common.cloudinary.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.SystemCode;

@Service
public class CloudinaryService {
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
                    ObjectUtils.emptyMap()
            );

            Object secureUrl = result.get("secure_url");
            if (secureUrl == null) {
                throw new AppException(SystemCode.UPLOAD_FAILED);
            }

            return secureUrl.toString();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(SystemCode.UPLOAD_FAILED);
        }
    }
}
