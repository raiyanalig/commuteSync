package com.commutesync.fleet.service;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceInUseException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.dto.CreateVehicleRequest;
import com.commutesync.fleet.dto.UpdateVehicleRequest;
import com.commutesync.fleet.dto.VehicleResponse;
import com.commutesync.fleet.repository.VehicleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public VehicleResponse create(CreateVehicleRequest request) {
        String registrationNumber = normalizeRegistration(request.registrationNumber());

        if (vehicleRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new DuplicateResourceException("Vehicle already registered: " + registrationNumber);
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setType(request.type());
        vehicle.setModel(trimToNull(request.model()));
        vehicle.setCapacity(request.capacity());
        vehicle.setStatus(request.status() == null ? VehicleStatus.AVAILABLE : request.status());

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> getAll(VehicleStatus status, String search, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        Page<Vehicle> page = vehicleRepository.search(status, normalizedSearch, pageable);
        return PageResponse.from(page.map(VehicleResponse::from));
    }

    @Transactional(readOnly = true)
    public VehicleResponse getById(Long id) {
        return VehicleResponse.from(findById(id));
    }

    @Transactional
    public VehicleResponse update(Long id, UpdateVehicleRequest request) {
        Vehicle vehicle = findById(id);
        String registrationNumber = normalizeRegistration(request.registrationNumber());

        if (!vehicle.getRegistrationNumber().equals(registrationNumber)
                && vehicleRepository.existsByRegistrationNumber(registrationNumber)) {
            throw new DuplicateResourceException("Vehicle already registered: " + registrationNumber);
        }

        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setType(request.type());
        vehicle.setModel(trimToNull(request.model()));
        vehicle.setCapacity(request.capacity());
        vehicle.setStatus(request.status());

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse updateStatus(Long id, VehicleStatus status) {
        Vehicle vehicle = findById(id);
        vehicle.setStatus(status);
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void delete(Long id) {
        Vehicle vehicle = findById(id);
        if (vehicle.getAssignedDriver() != null) {
            throw new ResourceInUseException(
                    "Vehicle has an assigned driver and cannot be deleted. Unassign first.");
        }
        vehicleRepository.delete(vehicle);
    }

    private Vehicle findById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }

    private String normalizeRegistration(String registrationNumber) {
        return registrationNumber.replaceAll("\\s+", "").toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
