package com.commutesync.fleet.service;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceInUseException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.DriverStatus;
import com.commutesync.fleet.dto.CreateDriverRequest;
import com.commutesync.fleet.dto.DriverResponse;
import com.commutesync.fleet.dto.UpdateDriverRequest;
import com.commutesync.fleet.repository.DriverRepository;
import com.commutesync.fleet.repository.VehicleRepository;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    public DriverService(DriverRepository driverRepository, VehicleRepository vehicleRepository) {
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional
    public DriverResponse create(CreateDriverRequest request) {
        String email = normalizeEmail(request.email());
        String licenseNumber = normalizeLicense(request.licenseNumber());

        if (driverRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }
        if (driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new DuplicateResourceException("License number already registered: " + licenseNumber);
        }

        Driver driver = new Driver();
        applyDetails(driver, request.fullName(), request.phone(), email,
                licenseNumber, request.licenseExpiryDate());
        driver.setStatus(request.status() == null ? DriverStatus.ACTIVE : request.status());
        driver.setAvailable(request.available() == null || request.available());

        return DriverResponse.from(driverRepository.save(driver));
    }

    @Transactional(readOnly = true)
    public PageResponse<DriverResponse> getAll(DriverStatus status, Boolean available,
                                               String search, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        Page<Driver> page = driverRepository.search(status, available, normalizedSearch, pageable);
        return PageResponse.from(page.map(DriverResponse::from));
    }

    @Transactional(readOnly = true)
    public DriverResponse getById(Long id) {
        return DriverResponse.from(findById(id));
    }

    @Transactional
    public DriverResponse update(Long id, UpdateDriverRequest request) {
        Driver driver = findById(id);
        String email = normalizeEmail(request.email());
        String licenseNumber = normalizeLicense(request.licenseNumber());

        if (!driver.getEmail().equals(email) && driverRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }
        if (!driver.getLicenseNumber().equals(licenseNumber)
                && driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new DuplicateResourceException("License number already registered: " + licenseNumber);
        }

        applyDetails(driver, request.fullName(), request.phone(), email,
                licenseNumber, request.licenseExpiryDate());
        driver.setStatus(request.status());
        driver.setAvailable(request.available());

        return DriverResponse.from(driverRepository.save(driver));
    }

    @Transactional
    public DriverResponse updateAvailability(Long id, boolean available) {
        Driver driver = findById(id);
        driver.setAvailable(available);
        return DriverResponse.from(driverRepository.save(driver));
    }

    @Transactional
    public void delete(Long id) {
        Driver driver = findById(id);
        if (vehicleRepository.existsByAssignedDriverId(id)) {
            throw new ResourceInUseException(
                    "Driver is assigned to a vehicle and cannot be deleted. Unassign first.");
        }
        driverRepository.delete(driver);
    }

    private Driver findById(Long id) {
        return driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));
    }

    private void applyDetails(Driver driver, String fullName, String phone, String email,
                              String licenseNumber, LocalDate licenseExpiryDate) {
        driver.setFullName(fullName.trim());
        driver.setPhone(phone.trim());
        driver.setEmail(email);
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseExpiryDate(licenseExpiryDate);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private String normalizeLicense(String licenseNumber) {
        return licenseNumber.trim().toUpperCase();
    }
}
