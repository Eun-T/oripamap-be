package org.scoula.common.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.*;

class S3ImageServiceTest {
    private PutObjectRequest uploadedRequest;
    private byte[] uploadedBytes;
    private boolean failUpload;
    private DeleteObjectRequest deletedRequest;
    private boolean failDelete;
    private final S3Client client = (S3Client) Proxy.newProxyInstance(
            S3Client.class.getClassLoader(), new Class<?>[]{S3Client.class}, (proxy, method, args) -> {
                if (method.getName().equals("putObject")) {
                    if (failUpload) {
                        throw S3Exception.builder().statusCode(403).message("Access denied").build();
                    }
                    uploadedRequest = (PutObjectRequest) args[0];
                    try (var input = ((RequestBody) args[1]).contentStreamProvider().newStream()) {
                        uploadedBytes = input.readAllBytes();
                    }
                    return PutObjectResponse.builder().build();
                }
                if (method.getName().equals("deleteObject")) {
                    if (failDelete) throw S3Exception.builder().statusCode(403).message("Access denied").build();
                    deletedRequest = (DeleteObjectRequest) args[0];
                    return DeleteObjectResponse.builder().build();
                }
                throw new UnsupportedOperationException(method.getName());
            });
    // 가짜 자격 증명으로 로컬 서명만 수행하며 AWS에는 접속하지 않는다.
    private final S3Presigner presigner = S3Presigner.builder()
            .region(Region.AP_SOUTHEAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create("test-access-key", "test-secret-key")))
            .build();
    private final S3ImageService service = new S3ImageService(client, presigner, "oripa-images");

    @AfterEach
    void closePresigner() {
        presigner.close();
    }

    @Test
    void uploadsDetectedImageTypeInsteadOfTrustingFilenameOrMimeType() throws Exception {
        for (String format : new String[]{"png", "jpg"}) {
            byte[] bytes = image(format);
            String key = service.upload(new MockMultipartFile("file", "../../fake.html", "text/html", bytes));

            assertTrue(key.matches("images/[0-9a-f-]{36}\\." + format));
            assertEquals("oripa-images", uploadedRequest.bucket());
            assertEquals(key, uploadedRequest.key());
            assertEquals(format.equals("jpg") ? "image/jpeg" : "image/png", uploadedRequest.contentType());
            assertNull(uploadedRequest.acl());
            assertArrayEquals(bytes, uploadedBytes);
        }
    }

    @Test
    void rejectsMissingEmptyAndDisguisedFilesBeforeUpload() {
        assertBadRequest(() -> service.upload(null));
        assertBadRequest(() -> service.upload(new MockMultipartFile("file", new byte[0])));
        assertBadRequest(() -> service.upload(new MockMultipartFile(
                "file", "fake.png", "image/png", "<html>not an image</html>".getBytes())));
        assertNull(uploadedRequest);
    }

    @Test
    void rejectsFilesOverFiveMegabytes() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", new byte[5 * 1024 * 1024 + 1])));
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, error.getStatus());
        assertNull(uploadedRequest);
    }

    @Test
    void wrapsS3FailureWithoutReturningAKey() throws Exception {
        failUpload = true;
        byte[] bytes = image("png");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.upload(new MockMultipartFile("file", bytes)));
        assertEquals(HttpStatus.BAD_GATEWAY, error.getStatus());
    }

    @Test
    void signsGetUrlForTenMinutesAndRejectsOtherKeys() {
        String key = "images/12345678-1234-1234-1234-123456789abc.png";
        String url = service.createPresignedGetUrl(key);
        assertTrue(url.startsWith("https://oripa-images.s3.ap-southeast-2.amazonaws.com/" + key + "?"));
        assertTrue(url.contains("X-Amz-Expires=600"));
        assertTrue(url.contains("X-Amz-Signature="));
        assertBadRequest(() -> service.createPresignedGetUrl(null));
        assertBadRequest(() -> service.createPresignedGetUrl("private/document.txt"));
        assertBadRequest(() -> service.createPresignedGetUrl("images/../document.png"));
    }

    @Test
    void deletesOnlyValidImageKeysFromConfiguredBucket() {
        String key = "images/12345678-1234-1234-1234-123456789abc.png";
        assertBadRequest(() -> service.delete("private/document.txt"));
        assertNull(deletedRequest);
        service.delete(key);
        assertEquals("oripa-images", deletedRequest.bucket());
        assertEquals(key, deletedRequest.key());
    }

    @Test
    void reportsS3DeleteFailure() {
        failDelete = true;
        assertEquals(HttpStatus.BAD_GATEWAY, assertThrows(ResponseStatusException.class,
                () -> service.delete("images/12345678-1234-1234-1234-123456789abc.png")).getStatus());
    }

    private byte[] image(String format) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output));
        return output.toByteArray();
    }

    private void assertBadRequest(org.junit.jupiter.api.function.Executable action) {
        assertEquals(HttpStatus.BAD_REQUEST,
                assertThrows(ResponseStatusException.class, action).getStatus());
    }
}
