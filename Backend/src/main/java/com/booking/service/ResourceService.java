package com.booking.service;

import com.booking.dto.request.ResourceRequest;
import com.booking.dto.response.ResourceResponse;
import com.booking.entity.Resource;
import com.booking.exception.ResourceNotFoundException;
import com.booking.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public List<ResourceResponse> findAll() {
        return resourceRepository.findAll()
                .stream()
                .map(ResourceResponse::from)
                .toList();
    }

    public ResourceResponse findById(Long id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));
        return ResourceResponse.from(resource);
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .name(request.name())
                .description(request.description())
                .type(request.type())
                .capacity(request.capacity())
                .location(request.location())
                .available(request.available() != null ? request.available() : true)
                .build();

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource", id));

        resource.setName(request.name());
        resource.setDescription(request.description());
        resource.setType(request.type());
        resource.setCapacity(request.capacity());
        resource.setLocation(request.location());

        if (request.available() != null) {
            resource.setAvailable(request.available());
        }

        return ResourceResponse.from(resourceRepository.save(resource));
    }

    @Transactional
    public void delete(Long id) {
        if (!resourceRepository.existsById(id)) {
            throw new ResourceNotFoundException("Resource", id);
        }
        resourceRepository.deleteById(id);
    }
}
