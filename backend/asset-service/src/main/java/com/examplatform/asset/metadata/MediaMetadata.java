package com.examplatform.asset.metadata;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Container for extracted media metadata.
 * Fields are nullable — only those applicable to the asset type will be populated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaMetadata {

    // Image
    private Integer width;
    private Integer height;
    private Integer dpi;
    private String orientation;

    // Audio / Video
    private Double durationSeconds;
    private String codec;
    private Integer bitrate;
    private Integer sampleRate;
    private Integer channels;

    // Video
    private Double frameRate;
}
