package mcc.survey.creator.service;

import mcc.survey.creator.dto.CreateServiceRequest;
import mcc.survey.creator.dto.ServiceDto;
import mcc.survey.creator.dto.UpdateServiceRequest;
import mcc.survey.creator.exception.DuplicateResourceException;
import mcc.survey.creator.exception.ResourceNotFoundException;
import mcc.survey.creator.model.Service;
import mcc.survey.creator.repository.ServiceRepository;
import mcc.survey.creator.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException; // For deleteService test

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private ServiceService serviceService;

    private Service service;
    private ServiceDto serviceDto;
    private CreateServiceRequest createServiceRequest;
    private UpdateServiceRequest updateServiceRequest;

    @BeforeEach
    void setUp() {
        service = new Service();
        service.setId(1L);
        service.setName("Test Service");
        service.setDescription("Test Description");

        serviceDto = new ServiceDto();
        serviceDto.setId(1L);
        serviceDto.setName("Test Service");
        serviceDto.setDescription("Test Description");

        createServiceRequest = new CreateServiceRequest();
        createServiceRequest.setName("Test Service");
        createServiceRequest.setDescription("Test Description");

        updateServiceRequest = new UpdateServiceRequest();
        updateServiceRequest.setName("Updated Service");
        updateServiceRequest.setDescription("Updated Description");
    }

    @Test
    void createService_success() {
        when(serviceRepository.existsByName(anyString())).thenReturn(false);
        when(serviceRepository.save(any(Service.class))).thenReturn(service);

        ServiceDto result = serviceService.createService(createServiceRequest);

        assertNotNull(result);
        assertEquals(serviceDto.getName(), result.getName());
        verify(serviceRepository).existsByName("Test Service");
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    void createService_duplicateName_throwsDuplicateResourceException() {
        when(serviceRepository.existsByName(anyString())).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> serviceService.createService(createServiceRequest));
        verify(serviceRepository).existsByName("Test Service");
        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    void getServiceEntityById_success_returnsServiceEntity() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        Service result = serviceService.getServiceEntityById(1L);
        assertNotNull(result);
        assertEquals(service.getName(), result.getName());
    }

    @Test
    void getServiceById_success() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));

        ServiceDto result = serviceService.getServiceById(1L);

        assertNotNull(result);
        assertEquals(serviceDto.getName(), result.getName());
        verify(serviceRepository).findById(1L);
    }

    @Test
    void getServiceById_notFound_throwsResourceNotFoundException() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceService.getServiceById(1L));
        verify(serviceRepository).findById(1L);
    }

    @Test
    void getServiceEntityById_notFound_throwsResourceNotFoundException() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> serviceService.getServiceEntityById(1L));
    }


    @Test
    void getAllServices_emptyList() {
        when(serviceRepository.findAll()).thenReturn(Collections.emptyList());

        List<ServiceDto> result = serviceService.getAllServices();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(serviceRepository).findAll();
    }

    @Test
    void getAllServices_returnsListOfServiceDtos() {
        when(serviceRepository.findAll()).thenReturn(List.of(service));

        List<ServiceDto> result = serviceService.getAllServices();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(serviceDto.getName(), result.get(0).getName());
        verify(serviceRepository).findAll();
    }

    @Test
    void updateService_success() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(serviceRepository.existsByName("Updated Service")).thenReturn(false);
        when(serviceRepository.save(any(Service.class))).thenAnswer(invocation -> invocation.getArgument(0));


        ServiceDto result = serviceService.updateService(1L, updateServiceRequest);

        assertNotNull(result);
        assertEquals("Updated Service", result.getName());
        assertEquals("Updated Description", result.getDescription());
        verify(serviceRepository).findById(1L);
        verify(serviceRepository).existsByName("Updated Service");
        verify(serviceRepository).save(any(Service.class));
    }

    @Test
    void updateService_onlyDescription_success() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        UpdateServiceRequest partialUpdateRequest = new UpdateServiceRequest();
        partialUpdateRequest.setDescription("Only Desc Updated");
        // Name is null in request

        when(serviceRepository.save(any(Service.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceDto result = serviceService.updateService(1L, partialUpdateRequest);

        assertNotNull(result);
        assertEquals(service.getName(), result.getName()); // Name should not change
        assertEquals("Only Desc Updated", result.getDescription());
        verify(serviceRepository).findById(1L);
        verify(serviceRepository, never()).existsByName(anyString()); // Name check should be skipped
        verify(serviceRepository).save(any(Service.class));
    }


    @Test
    void updateService_notFound_throwsResourceNotFoundException() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceService.updateService(1L, updateServiceRequest));
        verify(serviceRepository).findById(1L);
        verify(serviceRepository, never()).existsByName(anyString());
        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    void updateService_nameConflict_throwsDuplicateResourceException() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(serviceRepository.existsByName("Updated Service")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> serviceService.updateService(1L, updateServiceRequest));
        verify(serviceRepository).findById(1L);
        verify(serviceRepository).existsByName("Updated Service");
        verify(serviceRepository, never()).save(any(Service.class));
    }

    @Test
    void deleteService_success() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(teamRepository.findByService(service)).thenReturn(Collections.emptyList());
        doNothing().when(serviceRepository).delete(service);

        assertDoesNotThrow(() -> serviceService.deleteService(1L));

        verify(serviceRepository).findById(1L);
        verify(teamRepository).findByService(service);
        verify(serviceRepository).delete(service);
    }

    @Test
    void deleteService_notFound_throwsResourceNotFoundException() {
        when(serviceRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> serviceService.deleteService(1L));
        verify(serviceRepository).findById(1L);
        verify(teamRepository, never()).findByService(any(Service.class));
        verify(serviceRepository, never()).delete(any(Service.class));
    }

    @Test
    void deleteService_hasAssociatedTeams_throwsDuplicateResourceException() {
        // Based on current ServiceService implementation, it throws DuplicateResourceException
        // if teams are associated, not DataIntegrityViolationException directly from this method.
        // The message in ServiceService: "Cannot delete service with id '...' as it has associated teams."
        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(teamRepository.findByService(service)).thenReturn(List.of(new mcc.survey.creator.model.Team())); // Non-empty list

        assertThrows(DuplicateResourceException.class, () -> serviceService.deleteService(1L));
        verify(serviceRepository).findById(1L);
        verify(teamRepository).findByService(service);
        verify(serviceRepository, never()).delete(any(Service.class));
    }
}
