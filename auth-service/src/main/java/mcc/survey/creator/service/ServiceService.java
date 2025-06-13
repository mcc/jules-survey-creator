package mcc.survey.creator.service;

import mcc.survey.creator.dto.CreateServiceRequest;
import mcc.survey.creator.dto.ServiceDto;
import mcc.survey.creator.dto.UpdateServiceRequest;
import mcc.survey.creator.exception.DuplicateResourceException;
import mcc.survey.creator.exception.ResourceNotFoundException;
import mcc.survey.creator.model.Service;
import mcc.survey.creator.repository.ServiceRepository;
import mcc.survey.creator.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final TeamRepository teamRepository;

    @Autowired
    public ServiceService(ServiceRepository serviceRepository, TeamRepository teamRepository) {
        this.serviceRepository = serviceRepository;
        this.teamRepository = teamRepository;
    }

    @Transactional
    public ServiceDto createService(CreateServiceRequest request) {
        if (serviceRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Service with name '" + request.getName() + "' already exists.");
        }
        Service service = new Service();
        service.setName(request.getName());
        service.setDescription(request.getDescription());
        return mapToServiceDto(serviceRepository.save(service));
    }

    @Transactional(readOnly = true)
    public ServiceDto getServiceById(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .map(this::mapToServiceDto)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
    }

    @Transactional(readOnly = true)
    public mcc.survey.creator.model.Service getServiceEntityById(Long serviceId) {
        return serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found with id: " + serviceId));
    }

    @Transactional(readOnly = true)
    public List<ServiceDto> getAllServices() {
        return mapToServiceDtoList(serviceRepository.findAll());
    }

    @Transactional
    public ServiceDto updateService(Long serviceId, UpdateServiceRequest request) {
        mcc.survey.creator.model.Service service = getServiceEntityById(serviceId);

        if (request.getName() != null && !request.getName().equals(service.getName())) {
            if (serviceRepository.existsByName(request.getName())) {
                throw new DuplicateResourceException("Service with name '" + request.getName() + "' already exists.");
            }
            service.setName(request.getName());
        }
        if (request.getDescription() != null) {
            service.setDescription(request.getDescription());
        }
        return mapToServiceDto(serviceRepository.save(service));
    }

    @Transactional
    public void deleteService(Long serviceId) {
        mcc.survey.creator.model.Service service = getServiceEntityById(serviceId);
        if (!teamRepository.findByService(service).isEmpty()) {
            throw new DuplicateResourceException("Cannot delete service with id '" + serviceId + "' as it has associated teams.");
        }
        serviceRepository.delete(service);
    }

    public ServiceDto mapToServiceDto(mcc.survey.creator.model.Service service) {
        ServiceDto dto = new ServiceDto();
        dto.setId(service.getId());
        dto.setName(service.getName());
        dto.setDescription(service.getDescription());
        return dto;
    }

    public List<ServiceDto> mapToServiceDtoList(List<mcc.survey.creator.model.Service> services) {
        return services.stream().map(this::mapToServiceDto).collect(Collectors.toList());
    }
}
