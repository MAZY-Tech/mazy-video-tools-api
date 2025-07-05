package com.mazy.videotools.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mazy.videotools.dto.PresignRequestDTO;
import com.mazy.videotools.entity.VideoEvent;
import com.mazy.videotools.service.VideoEventService;
import com.mazy.videotools.service.impl.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.mazy.videotools.config.TestSecurityConfig;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = UploadController.class)
@Import(TestSecurityConfig.class)
public class UploadControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private S3Service s3Service;

    @MockBean
    private VideoEventService videoEventService;

    private PresignRequestDTO validRequest;
    private final String TEST_USER_ID = "test-user-id";
    private final String TEST_VIDEO_ID = UUID.randomUUID().toString();
    private final String TEST_BUCKET = "test-bucket";

    @BeforeEach
    void setUp() throws Exception {
        // Set up a valid request
        validRequest = new PresignRequestDTO();
        validRequest.setFilename("test-video.mp4");
        validRequest.setContentType("video/mp4");
        validRequest.setSizeBytes(1024L * 1024L); // 1MB
        validRequest.setDurationSeconds(60); // 1 minute
        validRequest.setXAmzMetaVideoHash("test-hash");

        // Mock S3Service
        when(s3Service.getBucketName()).thenReturn(TEST_BUCKET);
        when(s3Service.generatePresignedPutUrl(anyString(), anyString(), anyLong(), anyMap()))
                .thenReturn(new URL("https://test-bucket.s3.amazonaws.com/test-key"));

        // Mock VideoEventService
        VideoEvent mockVideoEvent = VideoEvent.builder()
                .videoId(TEST_VIDEO_ID)
                .bucket(TEST_BUCKET)
                .key("test-key")
                .fileName(validRequest.getFilename())
                .cognitoUserId(TEST_USER_ID)
                .build();

        when(videoEventService.createVideoEvent(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockVideoEvent);
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void createPresignedUrl_WithValidRequest_ShouldReturnOk() throws Exception {
        mockMvc.perform(post("/api/presign-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value(TEST_VIDEO_ID))
                .andExpect(jsonPath("$.uploadUrl").exists())
                .andExpect(jsonPath("$.s3Key").exists())
                .andExpect(jsonPath("$.expiresInMinutes").value(15));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void createPresignedUrl_WithInvalidExtension_ShouldReturnBadRequest() throws Exception {
        // Set invalid extension
        validRequest.setFilename("test-video.txt");

        mockMvc.perform(post("/api/presign-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Unsupported video format"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void createPresignedUrl_WithExcessiveSize_ShouldReturnBadRequest() throws Exception {
        // Set excessive size (3GB)
        validRequest.setSizeBytes(3L * 1024L * 1024L * 1024L);

        mockMvc.perform(post("/api/presign-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("File exceeds size limit of 2 GB"));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void createPresignedUrl_WithExcessiveDuration_ShouldReturnBadRequest() throws Exception {
        // Set excessive duration (70 minutes)
        validRequest.setDurationSeconds(70 * 60);

        mockMvc.perform(post("/api/presign-upload")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Video duration exceeds 60 minutes"));
    }
}
