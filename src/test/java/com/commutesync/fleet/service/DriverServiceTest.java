package com.commutesync.fleet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DriverServiceTest {

    private static final LocalDate FUTURE_EXPIRY = LocalDate.now().plusYears(1);

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private DriverService driverService;

    @Test
    void createNormalizesAndSavesDriver() {
        when(driverRepository.existsByEmail("raiyan@example.com")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("DL-12345")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> {
            Driver saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        DriverResponse response = driverService.create(new CreateDriverRequest(
                "Raiyan Ali", "+919876543210", "Raiyan@Example.com", "dl-12345",
                FUTURE_EXPIRY, null, null));

        assertThat(response.email()).isEqualTo("raiyan@example.com");
        assertThat(response.licenseNumber()).isEqualTo("DL-12345");
        assertThat(response.status()).isEqualTo(DriverStatus.ACTIVE);
        assertThat(response.available()).isTrue();
        assertThat(response.assignable()).isTrue();
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(driverRepository.existsByEmail("raiyan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> driverService.create(new CreateDriverRequest(
                "Raiyan Ali", "+919876543210", "raiyan@example.com", "DL-12345",
                FUTURE_EXPIRY, null, null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void createRejectsDuplicateLicense() {
        when(driverRepository.existsByEmail("raiyan@example.com")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("DL-12345")).thenReturn(true);

        assertThatThrownBy(() -> driverService.create(new CreateDriverRequest(
                "Raiyan Ali", "+919876543210", "raiyan@example.com", "dl-12345",
                FUTURE_EXPIRY, null, null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("DL-12345");

        verify(driverRepository, never()).save(any(Driver.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(driverRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> driverService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChanges() {
        Driver existing = driver(1L, "raiyan@example.com", "DL-12345", DriverStatus.ACTIVE, true);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(driverRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(driverRepository.existsByLicenseNumber("DL-99999")).thenReturn(false);
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponse response = driverService.update(1L, new UpdateDriverRequest(
                "New Name", "+919876543210", "New@Example.com", "dl-99999",
                FUTURE_EXPIRY, DriverStatus.ON_LEAVE, false));

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.status()).isEqualTo(DriverStatus.ON_LEAVE);
        assertThat(response.available()).isFalse();
        assertThat(response.assignable()).isFalse();
    }

    @Test
    void updateAvailabilityTogglesFlag() {
        Driver existing = driver(1L, "raiyan@example.com", "DL-12345", DriverStatus.ACTIVE, true);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(driverRepository.save(any(Driver.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverResponse response = driverService.updateAvailability(1L, false);

        assertThat(response.available()).isFalse();
    }

    @Test
    void deleteRejectsWhenAssignedToVehicle() {
        Driver existing = driver(1L, "raiyan@example.com", "DL-12345", DriverStatus.ACTIVE, true);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vehicleRepository.existsByAssignedDriverId(1L)).thenReturn(true);

        assertThatThrownBy(() -> driverService.delete(1L))
                .isInstanceOf(ResourceInUseException.class);

        verify(driverRepository, never()).delete(any(Driver.class));
    }

    @Test
    void deleteRemovesWhenNotAssigned() {
        Driver existing = driver(1L, "raiyan@example.com", "DL-12345", DriverStatus.ACTIVE, true);
        when(driverRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(vehicleRepository.existsByAssignedDriverId(1L)).thenReturn(false);

        driverService.delete(1L);

        verify(driverRepository).delete(existing);
    }

    private Driver driver(Long id, String email, String licenseNumber,
                          DriverStatus status, boolean available) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setFullName("Raiyan Ali");
        driver.setPhone("+919876543210");
        driver.setEmail(email);
        driver.setLicenseNumber(licenseNumber);
        driver.setLicenseExpiryDate(FUTURE_EXPIRY);
        driver.setStatus(status);
        driver.setAvailable(available);
        return driver;
    }
}
