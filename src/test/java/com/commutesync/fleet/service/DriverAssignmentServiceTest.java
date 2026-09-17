package com.commutesync.fleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceInUseException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.DriverStatus;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import com.commutesync.fleet.dto.VehicleResponse;
import com.commutesync.fleet.repository.DriverRepository;
import com.commutesync.fleet.repository.VehicleRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverAssignmentServiceTest {

    private static final LocalDate FUTURE_EXPIRY = LocalDate.now().plusYears(1);

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DriverRepository driverRepository;

    @InjectMocks
    private DriverAssignmentService driverAssignmentService;

    @Test
    void assignsAvailableDriverToAvailableVehicle() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE, null);
        Driver driver = driver(2L, DriverStatus.ACTIVE, true, FUTURE_EXPIRY);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByAssignedDriverId(2L)).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = driverAssignmentService.assignDriverToVehicle(1L, 2L);

        assertThat(response.assignedDriver()).isNotNull();
        assertThat(response.assignedDriver().id()).isEqualTo(2L);
    }

    @Test
    void rejectsWhenVehicleAlreadyHasDriver() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE, driver(9L, DriverStatus.ACTIVE, true, FUTURE_EXPIRY));
        Driver driver = driver(2L, DriverStatus.ACTIVE, true, FUTURE_EXPIRY);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverAssignmentService.assignDriverToVehicle(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already has an assigned driver");

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void rejectsWhenVehicleNotAvailable() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.MAINTENANCE, null);
        Driver driver = driver(2L, DriverStatus.ACTIVE, true, FUTURE_EXPIRY);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverAssignmentService.assignDriverToVehicle(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not available");

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void rejectsWhenDriverNotActiveOrUnavailable() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE, null);
        Driver driver = driver(2L, DriverStatus.INACTIVE, true, FUTURE_EXPIRY);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverAssignmentService.assignDriverToVehicle(1L, 2L))
                .isInstanceOf(BusinessException.class);

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void rejectsWhenDriverLicenseExpired() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE, null);
        Driver driver = driver(2L, DriverStatus.ACTIVE, true, LocalDate.now().minusDays(1));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));

        assertThatThrownBy(() -> driverAssignmentService.assignDriverToVehicle(1L, 2L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("license expiry");

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void rejectsWhenDriverAlreadyAssignedElsewhere() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE, null);
        Driver driver = driver(2L, DriverStatus.ACTIVE, true, FUTURE_EXPIRY);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(driverRepository.findById(2L)).thenReturn(Optional.of(driver));
        when(vehicleRepository.existsByAssignedDriverId(2L)).thenReturn(true);

        assertThatThrownBy(() -> driverAssignmentService.assignDriverToVehicle(1L, 2L))
                .isInstanceOf(ResourceInUseException.class);

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void rejectsWhenVehicleMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverAssignmentService.assignDriverToVehicle(99L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unassignsDriver() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.IN_USE, driver(2L, DriverStatus.ACTIVE, true, FUTURE_EXPIRY));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = driverAssignmentService.unassignDriverFromVehicle(1L);

        assertThat(response.assignedDriver()).isNull();
    }

    @Test
    void rejectsUnassignWhenNoDriver() {
        Vehicle vehicle = vehicle(1L, VehicleStatus.AVAILABLE, null);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> driverAssignmentService.unassignDriverFromVehicle(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no assigned driver");

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    private Vehicle vehicle(Long id, VehicleStatus status, Driver assignedDriver) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber("PB10AB1234");
        vehicle.setType(VehicleType.VAN);
        vehicle.setModel("Force");
        vehicle.setCapacity(12);
        vehicle.setStatus(status);
        vehicle.setAssignedDriver(assignedDriver);
        return vehicle;
    }

    private Driver driver(Long id, DriverStatus status, boolean available, LocalDate expiry) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setFullName("Driver " + id);
        driver.setPhone("+919876543210");
        driver.setEmail("driver" + id + "@example.com");
        driver.setLicenseNumber("DL-" + id);
        driver.setLicenseExpiryDate(expiry);
        driver.setStatus(status);
        driver.setAvailable(available);
        return driver;
    }
}
