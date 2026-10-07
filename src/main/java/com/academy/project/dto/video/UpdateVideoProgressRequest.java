package com.academy.project.dto.video;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVideoProgressRequest {

    /** Farthest playback position reached (seconds). */
    @NotNull(message = "watchedSeconds is required")
    @Min(value = 0, message = "watchedSeconds must be >= 0")
    private Integer watchedSeconds;

    /**
     * Actual video length from the player (seconds).
     * Optional if the video has durationMinutes set in admin.
     */
    @Min(value = 1, message = "durationSeconds must be >= 1")
    private Integer durationSeconds;
}
