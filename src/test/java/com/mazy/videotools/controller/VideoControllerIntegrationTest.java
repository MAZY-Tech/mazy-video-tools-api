package com.mazy.videotools.controller;

import com.mazy.videotools.dto.VideoDTO;
import com.mazy.videotools.entity.VideoEvent;
import com.mazy.videotools.entity.VideoStatus;
import com.mazy.videotools.service.VideoEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import com.mazy.videotools.config.TestSecurityConfig;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = VideoController.class)
@Import(TestSecurityConfig.class)
public class VideoControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VideoEventService videoEventService;

    private final String TEST_USER_ID = "test-user-id";
    private final String TEST_VIDEO_ID = UUID.randomUUID().toString();
    private final String TEST_BUCKET = "test-bucket";
    private final String TEST_KEY = "test-key";
    private final String TEST_FILENAME = "test-video.mp4";
    private final String TEST_DOWNLOAD_URL = "https://test-bucket.s3.amazonaws.com/test-key";

    private VideoEvent testVideoEvent;

    @BeforeEach
    void setUp() {
        // Create a test video event
        testVideoEvent = VideoEvent.builder()
                .videoId(TEST_VIDEO_ID)
                .bucket(TEST_BUCKET)
                .key(TEST_KEY)
                .fileName(TEST_FILENAME)
                .cognitoUserId(TEST_USER_ID)
                .status(VideoStatus.COMPLETED)
                .lastUpdate(LocalDateTime.now())
                .timestamp(OffsetDateTime.now())
                .build();

        // Set up a zip info
        VideoEvent.ZipInfo zipInfo = new VideoEvent.ZipInfo();
        zipInfo.setBucket(TEST_BUCKET);
        zipInfo.setKey(TEST_KEY);
        testVideoEvent.setZip(zipInfo);

        // Mock the service methods
        when(videoEventService.getVideoEventsByUserId(TEST_USER_ID))
                .thenReturn(Arrays.asList(testVideoEvent));

        when(videoEventService.getVideoEventByVideoId(TEST_VIDEO_ID))
                .thenReturn(testVideoEvent);

        when(videoEventService.getVideoDownloadUrl(any(VideoEvent.class)))
                .thenReturn(TEST_DOWNLOAD_URL);
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void getAllVideos_ShouldReturnListOfVideos() throws Exception {
        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videos", hasSize(1)))
                .andExpect(jsonPath("$.videos[0].video_id").value(TEST_VIDEO_ID))
                .andExpect(jsonPath("$.videos[0].file_name").value(TEST_FILENAME))
                .andExpect(jsonPath("$.videos[0].status").value(VideoStatus.COMPLETED.toString()))
                .andExpect(jsonPath("$.videos[0].download_url").value(TEST_DOWNLOAD_URL));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void getVideo_WithValidId_ShouldReturnVideo() throws Exception {
        mockMvc.perform(get("/api/videos/" + TEST_VIDEO_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.video_id").value(TEST_VIDEO_ID))
                .andExpect(jsonPath("$.file_name").value(TEST_FILENAME))
                .andExpect(jsonPath("$.status").value(VideoStatus.COMPLETED.toString()))
                .andExpect(jsonPath("$.download_url").value(TEST_DOWNLOAD_URL));
    }

    @Test
    @WithMockUser(username = TEST_USER_ID)
    void getVideo_WithInvalidId_ShouldReturnNotFound() throws Exception {
        // Mock service to return null for invalid ID
        when(videoEventService.getVideoEventByVideoId("invalid-id"))
                .thenReturn(null);

        mockMvc.perform(get("/api/videos/invalid-id"))
                .andExpect(status().isNotFound());
    }
}
