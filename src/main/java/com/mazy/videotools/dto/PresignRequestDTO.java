package com.mazy.videotools.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PresignRequestDTO {

    @NotBlank
    private String filename;

    @NotNull
    @Positive
    private Long sizeBytes;

    @NotNull
    @Positive
    private Integer durationSeconds;

    @NotBlank
    @JsonProperty("Content-Type")
    private String contentType;

    @NotBlank
    @JsonProperty("x-amz-meta-video_hash")
    private String xAmzMetaVideoHash;
}