package com.sa.event_mng.faker;

import com.sa.event_mng.modules.blog.domain.model.BlogPost;
import com.sa.event_mng.modules.blog.domain.model.BlogStatus;
import com.sa.event_mng.modules.blog.domain.model.BlogTag;
import com.sa.event_mng.modules.blog.domain.repository.BlogPostRepository;
import com.sa.event_mng.modules.blog.domain.repository.BlogTagRepository;
import com.sa.event_mng.modules.event.domain.repository.EventRepository;
import com.sa.event_mng.modules.identity.domain.model.User;
import com.sa.event_mng.modules.identity.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BlogPostSeeder {

    private static final int POST_COUNT = 50;

    private final BlogPostRepository blogPostRepository;
    private final BlogTagRepository blogTagRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    private static final String[] THUMBNAILS = {
            "https://picsum.photos/seed/blog1/800/400",
            "https://picsum.photos/seed/blog2/800/400",
            "https://picsum.photos/seed/blog3/800/400",
            "https://picsum.photos/seed/blog4/800/400",
            "https://picsum.photos/seed/blog5/800/400"
    };

    private static final String[] TITLES = {
            "Top 5 sự kiện âm nhạc không thể bỏ lỡ năm 2025",
            "Hướng dẫn mua vé sự kiện trực tuyến an toàn",
            "Những lễ hội văn hóa đặc sắc tại Việt Nam",
            "Kinh nghiệm tổ chức sự kiện chuyên nghiệp",
            "Xu hướng sự kiện ngoài trời mùa hè 2025",
            "Cách chọn loại vé phù hợp cho từng sự kiện",
            "Sự kiện thể thao lớn nhất năm tại TP. Hồ Chí Minh",
            "Nghệ thuật đường phố và các festival sáng tạo",
            "Bí quyết tận hưởng trọn vẹn một buổi hòa nhạc",
            "Các workshop kỹ năng sống được yêu thích nhất"
    };

    private static final String[] SUMMARIES = {
            "Tổng hợp những sự kiện nổi bật và hấp dẫn nhất dành cho bạn trong năm nay.",
            "Chia sẻ kinh nghiệm và mẹo hay để có trải nghiệm sự kiện tốt nhất.",
            "Khám phá những điểm đến văn hóa và giải trí thú vị trên khắp Việt Nam.",
            "Hướng dẫn chi tiết giúp bạn chuẩn bị tốt nhất cho mọi sự kiện.",
            "Cập nhật xu hướng và thông tin mới nhất về các sự kiện trong nước."
    };

    private static final String CONTENT_TEMPLATE =
            "Đây là nội dung chi tiết của bài viết. Bài viết cung cấp thông tin hữu ích và đáng tin cậy " +
            "về chủ đề liên quan đến sự kiện và giải trí. Hãy theo dõi chúng tôi để cập nhật " +
            "những thông tin mới nhất và hấp dẫn nhất.";

    private final Random random = new Random();

    public void seed() {
        if (blogPostRepository.count() > 0) return;

        List<User> authors = userRepository.findByRoles_Name("ORGANIZER");
        List<BlogTag> tags = blogTagRepository.findAll();
        if (authors.isEmpty()) return;

        List<Long> eventIds = eventRepository.findAll().stream().map(e -> e.getId()).collect(Collectors.toList());
        BlogStatus[] statuses = BlogStatus.values();

        for (int i = 0; i < POST_COUNT; i++) {
            String title = TITLES[i % TITLES.length] + " #" + (i + 1);
            String slug = "bai-viet-" + (i + 1) + "-" + System.nanoTime();

            BlogStatus status = statuses[i % statuses.length];
            LocalDateTime publishedAt = status == BlogStatus.PUBLISHED
                    ? LocalDateTime.now().minusDays(random.nextInt(60))
                    : null;

            Set<BlogTag> postTags = new HashSet<>();
            if (!tags.isEmpty()) {
                int tagCount = 1 + random.nextInt(Math.min(3, tags.size()));
                List<BlogTag> shuffled = new ArrayList<>(tags);
                Collections.shuffle(shuffled, random);
                postTags.addAll(shuffled.subList(0, tagCount));
            }

            Set<Long> postEventIds = new HashSet<>();
            if (!eventIds.isEmpty()) {
                int eventCount = 1 + random.nextInt(Math.min(3, eventIds.size()));
                List<Long> shuffledEvents = new ArrayList<>(eventIds);
                Collections.shuffle(shuffledEvents, random);
                postEventIds.addAll(shuffledEvents.subList(0, eventCount));
            }

            blogPostRepository.save(BlogPost.builder()
                    .title(title)
                    .slug(slug)
                    .summary(SUMMARIES[i % SUMMARIES.length])
                    .content(CONTENT_TEMPLATE)
                    .thumbnail(THUMBNAILS[i % THUMBNAILS.length])
                    .eventIds(postEventIds)
                    .metaTitle(title)
                    .metaDescription(SUMMARIES[i % SUMMARIES.length])
                    .author(authors.get(random.nextInt(authors.size())))
                    .status(status)
                    .publishedAt(publishedAt)
                    .tags(postTags)
                    .build());
        }
    }
}
