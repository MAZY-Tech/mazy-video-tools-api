package com.mazy.videotools.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class S3ServiceTest {

    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        // Use dummy values for region and bucket
        s3Service = new S3Service("us-east-1", "test-bucket");
    }

    @Test
    void testGeneratePresignedPutUrl() {
        URL url = s3Service.generatePresignedPutUrl(
                "test-key",
                "video/mp4",
                1024L,
                Collections.emptyMap()
        );
        assertNotNull(url);
        assertTrue(url.toString().contains("test-bucket"));
    }

    @Test
    void testGeneratePresignedFromBucketKeyGetUrl() {
        URL url = s3Service.generatePresignedFromBucketKeyGetUrl(
                "test-bucket",
                "test-key",
                Duration.ofMinutes(5)
        );
        assertNotNull(url);
        assertTrue(url.toString().contains("test-bucket"));
    }
}