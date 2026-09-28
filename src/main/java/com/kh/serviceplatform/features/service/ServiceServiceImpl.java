package com.kh.serviceplatform.features.service;

import com.kh.serviceplatform.common.cache.CacheNames;
import com.kh.serviceplatform.common.exception.BadRequestException;
import com.kh.serviceplatform.common.exception.ForbiddenException;
import com.kh.serviceplatform.common.exception.ResourceNotFoundException;
import com.kh.serviceplatform.features.file.FileRepository;
import com.kh.serviceplatform.features.file.StoredFile;
import com.kh.serviceplatform.features.file.enums.FileType;
import com.kh.serviceplatform.features.provider.ProviderProfile;
import com.kh.serviceplatform.features.provider.ProviderProfileRepository;
import com.kh.serviceplatform.features.service.dto.CreateServiceRequest;
import com.kh.serviceplatform.features.service.dto.ServiceResponse;
import com.kh.serviceplatform.features.service.dto.UpdateServiceRequest;
import com.kh.serviceplatform.features.service.enums.ServiceCategory;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceServiceImpl implements ServiceService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id", "name", "price", "durationMinutes", "category", "createdAt", "updatedAt"
    );

    private final ServiceRepository serviceRepository;
    private final ProviderProfileRepository providerProfileRepository;
    private final FileRepository fileRepository;
    private final ServiceMapper mapper;

    @Override
    @CacheEvict(value = CacheNames.PUBLIC_SERVICES, allEntries = true)
    public ServiceResponse createService(UUID userId, CreateServiceRequest request) {
        ProviderProfile provider = providerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found for user: " + userId));

        StoredFile imageFile = null;
        if (request.imageFileId() != null) {
            imageFile = validateAndGetFile(request.imageFileId(), userId, FileType.SERVICE_IMAGE, "Service image");
        }

        ServiceOffer service = ServiceOffer.builder()
                .provider(provider)
                .name(request.name().trim())
                .description(request.description() != null ? request.description().trim() : null)
                .category(request.category())
                .price(request.price())
                .durationMinutes(request.durationMinutes())
                .imageFile(imageFile)
                .isAvailable(request.isAvailable() != null ? request.isAvailable() : true)
                .build();

        ServiceOffer saved = serviceRepository.save(service);
        log.info("Created service offering with ID: {} for provider ID: {}", saved.getId(), provider.getId());
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceResponse> getMyServices(UUID userId, Pageable pageable) {
        ProviderProfile provider = providerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Provider profile not found for user: " + userId));

        Pageable safePageable = sanitizePageable(pageable);
        return serviceRepository.findByProviderId(provider.getId(), safePageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getMyServiceById(UUID userId, UUID serviceId) {
        ServiceOffer service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + serviceId));

        validateOwnership(service, userId);
        return mapper.toResponse(service);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PUBLIC_SERVICES, key = "#serviceId"),
            @CacheEvict(value = CacheNames.PUBLIC_SERVICES, allEntries = true)
    })
    public ServiceResponse updateMyService(UUID userId, UUID serviceId, UpdateServiceRequest request) {
        ServiceOffer service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + serviceId));

        validateOwnership(service, userId);

        if (request.name() != null && !request.name().isBlank()) {
            service.setName(request.name().trim());
        }
        if (request.description() != null) {
            service.setDescription(request.description().trim());
        }
        if (request.category() != null) {
            service.setCategory(request.category());
        }
        if (request.price() != null) {
            service.setPrice(request.price());
        }
        if (request.durationMinutes() != null) {
            service.setDurationMinutes(request.durationMinutes());
        }
        if (request.imageFileId() != null) {
            StoredFile imageFile = validateAndGetFile(request.imageFileId(), userId, FileType.SERVICE_IMAGE, "Service image");
            service.setImageFile(imageFile);
        }
        if (request.isAvailable() != null) {
            service.setAvailable(request.isAvailable());
        }

        ServiceOffer saved = serviceRepository.save(service);
        log.info("Updated service ID: {} by provider user: {}", serviceId, userId);
        return mapper.toResponse(saved);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = CacheNames.PUBLIC_SERVICES, key = "#serviceId"),
            @CacheEvict(value = CacheNames.PUBLIC_SERVICES, allEntries = true)
    })
    public void deleteMyService(UUID userId, UUID serviceId) {
        ServiceOffer service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + serviceId));

        validateOwnership(service, userId);
        serviceRepository.delete(service);
        log.info("Deleted service ID: {} by provider user: {}", serviceId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = CacheNames.PUBLIC_SERVICES,
            key = "T(java.lang.String).format('catalog:%s:%s:%s:%s:%s:%d:%d:%s', " +
                  "#search, #category, #providerId, #minPrice, #maxPrice, " +
                  "#pageable.pageNumber, #pageable.pageSize, #pageable.sort)"
    )
    public Page<ServiceResponse> getPublicServices(
            String search,
            ServiceCategory category,
            UUID providerId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        Pageable safePageable = sanitizePageable(pageable);

        Specification<ServiceOffer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always only return available services to public
            predicates.add(cb.isTrue(root.get("isAvailable")));

            if (category != null) {
                predicates.add(cb.equal(root.get("category"), category));
            }

            if (providerId != null) {
                predicates.add(cb.equal(root.get("provider").get("id"), providerId));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };

        return serviceRepository.findAll(spec, safePageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.PUBLIC_SERVICES, key = "#serviceId")
    public ServiceResponse getPublicServiceById(UUID serviceId) {
        ServiceOffer service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with ID: " + serviceId));

        if (!service.isAvailable()) {
            throw new ResourceNotFoundException("Service is currently unavailable");
        }

        return mapper.toResponse(service);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ServiceResponse> getPublicServicesByProviderId(UUID providerId, Pageable pageable) {
        Pageable safePageable = sanitizePageable(pageable);
        return serviceRepository.findByProviderIdAndIsAvailableTrue(providerId, safePageable)
                .map(mapper::toResponse);
    }

    private void validateOwnership(ServiceOffer service, UUID userId) {
        if (service.getProvider() == null || service.getProvider().getUser() == null ||
                !service.getProvider().getUser().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to access or modify this service");
        }
    }

    private StoredFile validateAndGetFile(UUID fileId, UUID currentUserId, FileType expectedType, String fieldName) {
        if (fileId == null) {
            return null;
        }

        StoredFile file = fileRepository.findById(fileId)
                .orElseThrow(() -> new BadRequestException(fieldName + " not found with ID: " + fileId));

        if (!file.getOwner().getId().equals(currentUserId)) {
            throw new ForbiddenException("You do not own the uploaded " + fieldName);
        }

        if (file.getFileType() != expectedType) {
            throw new BadRequestException(fieldName + " must be of type " + expectedType + " but was " + file.getFileType());
        }

        return file;
    }

    private Pageable sanitizePageable(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        List<Sort.Order> validOrders = pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_PROPERTIES.contains(order.getProperty()))
                .toList();

        Sort validSort = validOrders.isEmpty()
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(validOrders);

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), validSort);
    }
}
