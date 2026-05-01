package com.sa.event_mng.modules.blog.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogPostRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    String title;

    String summary;

    String content;

    String thumbnail;
    
    Set<Long> eventIds;
    
    String metaTitle;
    
    String metaDescription;

    Set<Long> tagIds;
}
