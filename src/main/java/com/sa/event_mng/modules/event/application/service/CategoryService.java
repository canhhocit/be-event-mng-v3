package com.sa.event_mng.modules.event.application.service;

import com.sa.event_mng.modules.event.application.dto.request.CategoryRequest;
import com.sa.event_mng.modules.event.application.dto.response.CategoryResponse;
import com.sa.event_mng.shared.exception.AppException;
import com.sa.event_mng.shared.exception.ErrorCode;
import com.sa.event_mng.modules.event.application.mapper.CategoryMapper;
import com.sa.event_mng.modules.event.domain.model.Category;
import com.sa.event_mng.modules.event.domain.repository.CategoryRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse create(CategoryRequest request) {
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Cacheable(value = "categories")
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Cacheable(value = "category_by_id", key = "#id")
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return categoryMapper.toCategoryResponse(category);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"categories", "category_by_id"}, allEntries = true)
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = {"categories", "category_by_id"}, allEntries = true)
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }
}
