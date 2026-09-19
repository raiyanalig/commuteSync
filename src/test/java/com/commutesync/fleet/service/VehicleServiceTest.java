package com.commutesync.fleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.audit.service.AuditService;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceInUseException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.DriverStatus;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import com.commutesync.fleet.dto.CreateVehicleRequest;
import com.commutesync.fleet.dto.UpdateVehicleRequest;
import com.commutesync.fleet.dto.VehicleResponse;
import com.commutesync.fleet.repository.VehicleRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void createNormalizesRegistrationAndDefaultsStatus() {
        when(vehicleRepository.existsByRegistrationNumber("PB10AB1234")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> {
            Vehicle saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        VehicleResponse response = vehicleService.create(new CreateVehicleRequest(
                "pb10 ab 1234", VehicleType.VAN, "Force Traveller", 12, null));

        assertThat(response.registrationNumber()).isEqualTo("PB10AB1234");
        assertThat(response.status()).isEqualTo(VehicleStatus.AVAILABLE);
        assertThat(response.capacity()).isEqualTo(12);
        assertThat(response.assignedDriver()).isNull();
        assertThat(response.assignable()).isTrue();
    }

    @Test
    void createRejectsDuplicateRegistration() {
        when(vehicleRepository.existsByRegistrationNumber("PB10AB1234")).thenReturn(true);

        assertThatThrownBy(() -> vehicleService.create(new CreateVehicleRequest(
                "PB10AB1234", VehicleType.VAN, "Force", 12, null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChanges() {
        Vehicle existing = vehicle(1L, "PB10AB1234", VehicleStatus.AVAILABLE, null);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vehicleRepository.existsByRegistrationNumber("PB10XY9999")).thenReturn(false);
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.update(1L, new UpdateVehicleRequest(
                "pb10 xy 9999", VehicleType.BUS, "Volvo 9400", 45, VehicleStatus.MAINTENANCE));

        assertThat(response.registrationNumber()).isEqualTo("PB10XY9999");
        assertThat(response.type()).isEqualTo(VehicleType.BUS);
        assertThat(response.capacity()).isEqualTo(45);
        assertThat(response.status()).isEqualTo(VehicleStatus.MAINTENANCE);
    }

    @Test
    void updateStatusChangesStatus() {
        Vehicle existing = vehicle(1L, "PB10AB1234", VehicleStatus.AVAILABLE, null);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateStatus(1L, VehicleStatus.MAINTENANCE);

        assertThat(response.status()).isEqualTo(VehicleStatus.MAINTENANCE);
    }

    @Test
    void deleteRejectsWhenDriverAssigned() {
        Vehicle existing = vehicle(1L, "PB10AB1234", VehicleStatus.IN_USE, driver(2L));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> vehicleService.delete(1L))
                .isInstanceOf(ResourceInUseException.class);

        verify(vehicleRepository, never()).delete(any(Vehicle.class));
    }

    @Test
    void deleteRemovesWhenNoDriverAssigned() {
        Vehicle existing = vehicle(1L, "PB10AB1234", VehicleStatus.AVAILABLE, null);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(existing));

        vehicleService.delete(1L);

        verify(vehicleRepository).delete(existing);
    }

    private Vehicle vehicle(Long id, String registrationNumber, VehicleStatus status, Driver assignedDriver) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setType(VehicleType.VAN);
        vehicle.setModel("Force");
        vehicle.setCapacity(12);
        vehicle.setStatus(status);
        vehicle.setAssignedDriver(assignedDriver);
        return vehicle;
    }

    private Driver driver(Long id) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setFullName("Driver " + id);
        driver.setPhone("+919876543210");
        driver.setEmail("driver" + id + "@example.com");
        driver.setLicenseNumber("DL-" + id);
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(1));
        driver.setStatus(DriverStatus.ACTIVE);
        driver.setAvailable(true);
        return driver;
    }
}
