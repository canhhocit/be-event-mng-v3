package com.sa.event_mng.modules.blog.application.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogPostResponse {
    Long id;
    String title;
    String slug;
    String summary;
    String content;
    String thumbnail;
    Set<Long> eventIds;
    String metaTitle;
    String metaDescription;
    String authorName;
    String status;
    LocalDateTime publishedAt;
    LocalDateTime createdAt;
    Set<BlogTagResponse> tags;
}
