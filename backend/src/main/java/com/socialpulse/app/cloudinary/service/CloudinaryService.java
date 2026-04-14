package com.socialpulse.app.cloudinary.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.emptyMap()
            );

            Object secureUrl = result.get("secure_url");
            if (secureUrl == null) {
                throw new AppException(ErrorCode.UPLOAD_FAILED);
            }

            return secureUrl.toString();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            throw new AppException(ErrorCode.UPLOAD_FAILED);
        }
    }
}
