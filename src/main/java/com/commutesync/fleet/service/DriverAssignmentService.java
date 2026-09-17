package com.commutesync.fleet.service;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceInUseException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.dto.VehicleResponse;
import com.commutesync.fleet.repository.DriverRepository;
import com.commutesync.fleet.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverAssignmentService {

    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public DriverAssignmentService(VehicleRepository vehicleRepository,
                                   DriverRepository driverRepository) {
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional
    public VehicleResponse assignDriverToVehicle(Long vehicleId, Long driverId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));

        if (vehicle.getAssignedDriver() != null) {
            throw new BusinessException("Vehicle " + vehicle.getRegistrationNumber()
                    + " already has an assigned driver");
        }
        if (!vehicle.isAssignable()) {
            throw new BusinessException("Vehicle is not available for assignment (status: "
                    + vehicle.getStatus() + ")");
        }
        if (!driver.isAssignable()) {
            throw new BusinessException("Driver is not available for assignment (status: "
                    + driver.getStatus() + ", available: " + driver.isAvailable()
                    + ", license expiry: " + driver.getLicenseExpiryDate() + ")");
        }
        if (vehicleRepository.existsByAssignedDriverId(driverId)) {
            throw new ResourceInUseException("Driver is already assigned to another vehicle");
        }

        vehicle.setAssignedDriver(driver);
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse unassignDriverFromVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));

        if (vehicle.getAssignedDriver() == null) {
            throw new BusinessException("Vehicle " + vehicle.getRegistrationNumber()
                    + " has no assigned driver");
        }

        vehicle.setAssignedDriver(null);
        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }
}
