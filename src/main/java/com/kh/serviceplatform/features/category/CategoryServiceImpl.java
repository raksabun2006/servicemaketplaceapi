package com.kh.serviceplatform.features.category;

import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.category.dto.CategoryResponse;
import com.kh.serviceplatform.features.category.dto.CreateCategoryRequest;
import com.kh.serviceplatform.features.category.dto.UpdateCategoryRequest;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.file.StoredFile;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private static final Set<String> ALLOWED_SORTS = Set.of(
            "id", "name", "code", "displayOrder", "isActive", "createdAt", "updatedAt"
    );

    private final CategoryRepository categoryRepository;
    private final FileRepository fileRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAscNameAsc()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategories(String search, Boolean isActive, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);

        Specification<Category> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate codeMatch = cb.like(cb.lower(root.get("code")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameMatch, codeMatch, descMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return categoryRepository.findAll(spec, safePageable)
                .map(categoryMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Category code cannot be blank");
        }
        Category category = categoryRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with code: " + code));
        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String trimmedName = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new BadRequestException("Category with name '" + trimmedName + "' already exists");
        }

        String code = (request.code() != null && !request.code().isBlank())
                ? request.code().trim().toUpperCase(Locale.ROOT)
                : generateCodeFromName(trimmedName);

        if (categoryRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException("Category with code '" + code + "' already exists");
        }

        StoredFile iconFile = null;
        if (request.iconFileId() != null) {
            iconFile = fileRepository.findById(request.iconFileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Icon file not found with ID: " + request.iconFileId()));
        }

        Category category = Category.builder()
                .name(trimmedName)
                .code(code)
                .description(request.description() != null ? request.description().trim() : null)
                .iconFile(iconFile)
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .isActive(request.isActive() != null ? request.isActive() : true)
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Category created successfully. Id: {}, Name: {}, Code: {}", saved.getId(), saved.getName(), saved.getCode());
        return categoryMapper.toResponse(saved);
    }

    @Override
    public CategoryResponse updateCategory(UUID id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (request.name() != null && !request.name().isBlank()) {
            String trimmedName = request.name().trim();
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
                throw new BadRequestException("Category with name '" + trimmedName + "' already exists");
            }
            category.setName(trimmedName);
        }

        if (request.code() != null && !request.code().isBlank()) {
            String trimmedCode = request.code().trim().toUpperCase(Locale.ROOT);
            if (categoryRepository.existsByCodeIgnoreCaseAndIdNot(trimmedCode, id)) {
                throw new BadRequestException("Category with code '" + trimmedCode + "' already exists");
            }
            category.setCode(trimmedCode);
        }

        if (request.description() != null) {
            category.setDescription(request.description().trim());
        }

        if (request.iconFileId() != null) {
            StoredFile iconFile = fileRepository.findById(request.iconFileId())
                    .orElseThrow(() -> new ResourceNotFoundException("Icon file not found with ID: " + request.iconFileId()));
            category.setIconFile(iconFile);
        }

        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }

        if (request.isActive() != null) {
            category.setActive(request.isActive());
        }

        Category saved = categoryRepository.save(category);
        log.info("Category updated successfully. Id: {}, Name: {}, Code: {}", saved.getId(), saved.getName(), saved.getCode());
        return categoryMapper.toResponse(saved);
    }

    @Override
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        categoryRepository.delete(category);
        log.info("Category deleted successfully. Id: {}, Code: {}", id, category.getCode());
    }

    @Override
    public CategoryResponse toggleCategoryStatus(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        category.setActive(!category.isActive());
        Category saved = categoryRepository.save(category);
        log.info("Category status toggled. Id: {}, Active: {}", saved.getId(), saved.isActive());
        return categoryMapper.toResponse(saved);
    }

    private String generateCodeFromName(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String code = normalized.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return code.isEmpty() ? "CATEGORY_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT) : code;
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by(Sort.Direction.ASC, "name")));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by(Sort.Direction.ASC, "name")));
        }

        List<Sort.Order> validOrders = pageable.getSort().stream()
                .filter(order -> ALLOWED_SORTS.contains(order.getProperty()))
                .toList();

        Sort validSort = validOrders.isEmpty()
                ? Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by(Sort.Direction.ASC, "name"))
                : Sort.by(validOrders);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), validSort);
    }
}
