package com.sa.event_mng.modules.blog.application.mapper;

import com.sa.event_mng.modules.blog.application.dto.response.BlogPostResponse;
import com.sa.event_mng.modules.blog.application.dto.response.BlogTagResponse;
import com.sa.event_mng.modules.blog.domain.model.BlogPost;
import com.sa.event_mng.modules.blog.domain.model.BlogTag;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class BlogMapper {

    public BlogTagResponse toTagResponse(BlogTag tag) {
        return BlogTagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .build();
    }

    public BlogPostResponse toPostResponse(BlogPost post) {
        return BlogPostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .summary(post.getSummary())
                .content(post.getContent())
                .thumbnail(post.getThumbnail())
                .eventIds(post.getEventIds())
                .metaTitle(post.getMetaTitle())
                .metaDescription(post.getMetaDescription())
                .authorName(post.getAuthor() != null ? post.getAuthor().getFullName() : null)
                .status(post.getStatus().name())
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .tags(post.getTags().stream().map(this::toTagResponse).collect(Collectors.toSet()))
                .build();
    }
}
