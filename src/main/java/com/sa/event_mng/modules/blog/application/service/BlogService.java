package com.sa.event_mng.modules.blog.application.service;

import com.sa.event_mng.modules.blog.application.dto.request.BlogPostRequest;
import com.sa.event_mng.modules.blog.application.dto.request.BlogTagRequest;
import com.sa.event_mng.modules.blog.application.dto.response.BlogPostResponse;
import com.sa.event_mng.modules.blog.application.dto.response.BlogTagResponse;
import com.sa.event_mng.modules.blog.application.mapper.BlogMapper;
import com.sa.event_mng.modules.blog.domain.model.BlogPost;
import com.sa.event_mng.modules.blog.domain.model.BlogStatus;
import com.sa.event_mng.modules.blog.domain.model.BlogTag;
import com.sa.event_mng.modules.blog.domain.repository.BlogPostRepository;
import com.sa.event_mng.modules.blog.domain.repository.BlogTagRepository;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogService {

    BlogPostRepository blogPostRepository;
    BlogTagRepository blogTagRepository;
    UserRepository userRepository;
    BlogMapper blogMapper;

    // ==================== PUBLIC ====================

    public Page<BlogPostResponse> getPublishedPosts(int page, int size) {
        return blogPostRepository.findByStatus(BlogStatus.PUBLISHED, PageRequest.of(page, size))
                .map(blogMapper::toPostResponse);
    }

    public BlogPostResponse getPostBySlug(String slug) {
        return blogMapper.toPostResponse(
                blogPostRepository.findBySlug(slug)
                        .filter(p -> p.getStatus() == BlogStatus.PUBLISHED)
                        .orElseThrow(() -> new AppException(ErrorCode.BLOG_POST_NOT_FOUND))
        );
    }

    public List<BlogTagResponse> getAllTags() {
        return blogTagRepository.findAll().stream()
                .map(blogMapper::toTagResponse)
                .collect(Collectors.toList());
    }

    // ==================== ORGANIZER ====================

    @Transactional
    @PreAuthorize("hasRole('ORGANIZER')")
    public BlogPostResponse organizerCreatePost(BlogPostRequest request) {
        User author = getCurrentUser();
        BlogPost post = BlogPost.builder()
                .title(request.getTitle())
                .slug(generateUniqueSlug(request.getTitle()))
                .summary(request.getSummary())
                .content(request.getContent())
                .thumbnail(request.getThumbnail())
                .eventIds(request.getEventIds())
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .author(author)
                .tags(resolveTags(request.getTagIds()))
                .status(BlogStatus.DRAFT)
                .build();
        return blogMapper.toPostResponse(blogPostRepository.save(post));
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZER')")
    public BlogPostResponse organizerUpdatePost(Long id, BlogPostRequest request) {
        BlogPost post = getOwnedPost(id);
        if (post.getStatus() == BlogStatus.PUBLISHED)
            throw new AppException(ErrorCode.BLOG_POST_ALREADY_PUBLISHED);
        post.setTitle(request.getTitle());
        post.setSummary(request.getSummary());
        post.setContent(request.getContent());
        post.setThumbnail(request.getThumbnail());
        post.setEventIds(request.getEventIds());
        post.setMetaTitle(request.getMetaTitle());
        post.setMetaDescription(request.getMetaDescription());
        post.setTags(resolveTags(request.getTagIds()));
        post.setStatus(BlogStatus.DRAFT);
        return blogMapper.toPostResponse(blogPostRepository.save(post));
    }

    @Transactional
    @PreAuthorize("hasRole('ORGANIZER')")
    public void organizerDeletePost(Long id) {
        BlogPost post = getOwnedPost(id);
        if (post.getStatus() == BlogStatus.PUBLISHED)
            throw new AppException(ErrorCode.BLOG_POST_ALREADY_PUBLISHED);
        blogPostRepository.delete(post);
    }

    @PreAuthorize("hasRole('ORGANIZER')")
    public Page<BlogPostResponse> organizerGetMyPosts(int page, int size) {
        User author = getCurrentUser();
        return blogPostRepository.findByAuthorId(author.getId(), PageRequest.of(page, size))
                .map(blogMapper::toPostResponse);
    }

    // ==================== ADMIN ====================

    @PreAuthorize("hasRole('ADMIN')")
    public Page<BlogPostResponse> getAllPosts(int page, int size) {
        return blogPostRepository.findAll(PageRequest.of(page, size))
                .map(blogMapper::toPostResponse);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BlogPostResponse createPost(BlogPostRequest request) {
        User author = getCurrentUser();
        BlogPost post = BlogPost.builder()
                .title(request.getTitle())
                .slug(generateUniqueSlug(request.getTitle()))
                .summary(request.getSummary())
                .content(request.getContent())
                .thumbnail(request.getThumbnail())
                .eventIds(request.getEventIds())
                .metaTitle(request.getMetaTitle())
                .metaDescription(request.getMetaDescription())
                .author(author)
                .tags(resolveTags(request.getTagIds()))
                .build();
        return blogMapper.toPostResponse(blogPostRepository.save(post));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BlogPostResponse updatePost(Long id, BlogPostRequest request) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_POST_NOT_FOUND));
        post.setTitle(request.getTitle());
        post.setSummary(request.getSummary());
        post.setContent(request.getContent());
        post.setThumbnail(request.getThumbnail());
        post.setEventIds(request.getEventIds());
        post.setMetaTitle(request.getMetaTitle());
        post.setMetaDescription(request.getMetaDescription());
        post.setTags(resolveTags(request.getTagIds()));
        return blogMapper.toPostResponse(blogPostRepository.save(post));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BlogPostResponse publishPost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_POST_NOT_FOUND));
        post.setStatus(BlogStatus.PUBLISHED);
        post.setPublishedAt(LocalDateTime.now());
        return blogMapper.toPostResponse(blogPostRepository.save(post));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BlogPostResponse rejectPost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_POST_NOT_FOUND));
        post.setStatus(BlogStatus.REJECTED);
        return blogMapper.toPostResponse(blogPostRepository.save(post));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BlogPostResponse archivePost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_POST_NOT_FOUND));
        post.setStatus(BlogStatus.ARCHIVED);
        return blogMapper.toPostResponse(blogPostRepository.save(post));
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void deletePost(Long id) {
        if (!blogPostRepository.existsById(id))
            throw new AppException(ErrorCode.BLOG_POST_NOT_FOUND);
        blogPostRepository.deleteById(id);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public BlogTagResponse createTag(BlogTagRequest request) {
        if (blogTagRepository.existsByName(request.getName()))
            throw new AppException(ErrorCode.BLOG_TAG_EXISTED);
        return blogMapper.toTagResponse(blogTagRepository.save(
                BlogTag.builder().name(request.getName()).slug(request.getSlug()).build()
        ));
    }

    // ==================== HELPER ====================

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private BlogPost getOwnedPost(Long id) {
        BlogPost post = blogPostRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_POST_NOT_FOUND));
        User current = getCurrentUser();
        if (!post.getAuthor().getId().equals(current.getId()))
            throw new AppException(ErrorCode.UNAUTHORIZED);
        return post;
    }

    private Set<BlogTag> resolveTags(Set<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return new HashSet<>();
        return new HashSet<>(blogTagRepository.findAllById(tagIds));
    }

    private String generateUniqueSlug(String title) {
        String base = Normalizer.normalize(title, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        String slug = base;
        int count = 1;
        while (blogPostRepository.existsBySlug(slug)) {
            slug = base + "-" + count++;
        }
        return slug;
    }
}
