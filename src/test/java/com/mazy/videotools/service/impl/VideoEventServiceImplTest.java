package com.mazy.videotools.service.impl;

import com.mazy.videotools.entity.VideoEvent;
import com.mazy.videotools.entity.VideoStatus;
import com.mazy.videotools.repository.VideoEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VideoEventServiceImplTest {

    @Mock
    private VideoEventRepository videoEventRepository;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private VideoEventServiceImpl videoEventService;

    private VideoEvent testVideoEvent;
    private final String TEST_BUCKET = "test-bucket";
    private final String TEST_KEY = "test-key";
    private final String TEST_FILENAME = "test-file.mp4";
    private final String TEST_USER_ID = "test-user-id";
    private final String TEST_VIDEO_ID = "test-video-id";

    @BeforeEach
    void setUp() {
        testVideoEvent = VideoEvent.builder()
                .videoId(TEST_VIDEO_ID)
                .bucket(TEST_BUCKET)
                .key(TEST_KEY)
                .fileName(TEST_FILENAME)
                .cognitoUserId(TEST_USER_ID)
                .status(VideoStatus.INITIAL)
                .build();
    }

    @Test
    void createVideoEvent_ShouldCreateAndSaveVideoEvent() {
        // Arrange
        when(videoEventRepository.save(any(VideoEvent.class))).thenReturn(testVideoEvent);

        // Act
        VideoEvent result = videoEventService.createVideoEvent(TEST_BUCKET, TEST_KEY, TEST_FILENAME, TEST_USER_ID);

        // Assert
        assertNotNull(result);
        verify(videoEventRepository, times(1)).save(any(VideoEvent.class));
        assertEquals(VideoStatus.INITIAL, result.getStatus());
    }

    @Test
    void getVideoEventByVideoId_ShouldReturnVideoEvent_WhenExists() {
        // Arrange
        when(videoEventRepository.findByVideoId(TEST_VIDEO_ID)).thenReturn(Optional.of(testVideoEvent));

        // Act
        VideoEvent result = videoEventService.getVideoEventByVideoId(TEST_VIDEO_ID);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_VIDEO_ID, result.getVideoId());
        verify(videoEventRepository, times(1)).findByVideoId(TEST_VIDEO_ID);
    }

    @Test
    void getVideoEventByVideoId_ShouldReturnNull_WhenNotExists() {
        // Arrange
        when(videoEventRepository.findByVideoId(TEST_VIDEO_ID)).thenReturn(Optional.empty());

        // Act
        VideoEvent result = videoEventService.getVideoEventByVideoId(TEST_VIDEO_ID);

        // Assert
        assertNull(result);
        verify(videoEventRepository, times(1)).findByVideoId(TEST_VIDEO_ID);
    }

    @Test
    void getVideoDownloadUrl_ShouldReturnNull_WhenZipIsNull() {
        // Act
        String result = videoEventService.getVideoDownloadUrl(testVideoEvent);

        // Assert
        assertNull(result);
    }

    @Test
    void getVideoEventsByUserId_ShouldReturnListOfVideoEvents() {
        // Arrange
        List<VideoEvent> expectedEvents = Arrays.asList(testVideoEvent);
        when(videoEventRepository.findVideoEventByCognitoUserId(TEST_USER_ID)).thenReturn(expectedEvents);

        // Act
        List<VideoEvent> result = videoEventService.getVideoEventsByUserId(TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TEST_VIDEO_ID, result.get(0).getVideoId());
        verify(videoEventRepository, times(1)).findVideoEventByCognitoUserId(TEST_USER_ID);
    }

    @Test
    void getVideoDownloadUrl_ShouldReturnPresignedUrl_WhenZipIsNotNull() throws MalformedURLException {
        // Arrange
        VideoEvent.ZipInfo zip = new VideoEvent.ZipInfo();

        zip.setBucket(TEST_BUCKET);
        zip.setKey(TEST_KEY);
        testVideoEvent.setZip(zip);

        String expectedUrl = "https://presigned-url.com";
        when(s3Service.generatePresignedFromBucketKeyGetUrl(eq(TEST_BUCKET), eq(TEST_KEY), any(Duration.class)))
                .thenReturn(new URL(expectedUrl));

        // Act
        String result = videoEventService.getVideoDownloadUrl(testVideoEvent);

        // Assert
        assertNotNull(result);
        assertEquals(expectedUrl, result);
        verify(s3Service, times(1)).generatePresignedFromBucketKeyGetUrl(eq(TEST_BUCKET), eq(TEST_KEY), any(Duration.class));
    }
}