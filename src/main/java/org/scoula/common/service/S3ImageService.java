package org.scoula.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class S3ImageService {
    private static final int MAX_FILE_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 20_000_000;
    private static final Duration URL_DURATION = Duration.ofMinutes(10);
    private static final Pattern IMAGE_KEY = Pattern.compile(
            "images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3ImageService(S3Client s3Client, S3Presigner s3Presigner,
                          @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    /** 이미지를 업로드하고 DB에 저장할 객체 키를 반환한다. */
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("이미지 파일을 선택해 주세요.");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw tooLarge();
        }

        byte[] bytes;
        try (InputStream input = file.getInputStream()) {
            // MultipartFile의 크기 정보와 무관하게 실제 읽는 양도 제한한다.
            bytes = input.readNBytes(MAX_FILE_BYTES + 1);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 읽을 수 없습니다.", e);
        }
        if (bytes.length > MAX_FILE_BYTES) {
            throw tooLarge();
        }
        String extension = validateImage(bytes);
        String key = "images/" + UUID.randomUUID() + "." + extension;
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentTypeFor(extension))
                .contentLength((long) bytes.length)
                .build();
        try {
            s3Client.putObject(request, RequestBody.fromBytes(bytes));
            return key;
        } catch (SdkException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "이미지 업로드에 실패했습니다.", e);
        }
    }

    /** DB에서 확인한 객체 키만 전달한다. 이미 없는 객체의 삭제도 성공으로 처리된다. */
    public void delete(String key) {
        if (key == null || !IMAGE_KEY.matcher(key).matches()) {
            throw badRequest("유효하지 않은 이미지 키입니다.");
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "이미지 삭제에 실패했습니다.", e);
        }
    }

    /** 호출자가 접근 권한을 확인한 객체 키에 대해 임시 조회 URL을 발급한다. */
    public String createPresignedGetUrl(String key) {
        if (key == null || !IMAGE_KEY.matcher(key).matches()) {
            throw badRequest("유효하지 않은 이미지 키입니다.");
        }
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        try {
            return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .getObjectRequest(request)
                            .signatureDuration(URL_DURATION)
                            .build())
                    .url().toExternalForm();
        } catch (SdkException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "이미지 조회 URL 생성에 실패했습니다.", e);
        }
    }

    private String validateImage(byte[] bytes) {
        try (MemoryCacheImageInputStream input = new MemoryCacheImageInputStream(
                new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw badRequest("JPG, PNG 또는 WebP 이미지 파일만 업로드할 수 있습니다.");
            }
            ImageReader reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("jpeg") && !format.equals("png") && !format.equals("webp")) {
                    throw badRequest("JPG, PNG 또는 WebP 이미지 파일만 업로드할 수 있습니다.");
                }
                reader.setInput(input);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw badRequest("이미지는 최대 2,000만 픽셀까지 업로드할 수 있습니다.");
                }
                // 헤더만 이미지인 파일도 거르기 위해 디코딩까지 확인한다.
                if (reader.read(0) == null) {
                    throw badRequest("유효한 이미지 파일이 아닙니다.");
                }
                return format.equals("jpeg") ? "jpg" : format;
            } finally {
                reader.dispose();
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "손상되었거나 유효하지 않은 이미지입니다.", e);
        }
    }

    private String contentTypeFor(String extension) {
        return switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new IllegalArgumentException("Unsupported image extension: " + extension);
        };
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException tooLarge() {
        return new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "이미지는 최대 5MB까지 업로드할 수 있습니다.");
    }
}
