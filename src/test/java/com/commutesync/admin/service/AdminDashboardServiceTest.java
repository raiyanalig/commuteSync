package com.commutesync.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.commutesync.admin.dto.DashboardResponse;
import com.commutesync.admin.dto.FleetSummaryResponse;
import com.commutesync.admin.dto.TripStatusSummaryResponse;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.booking.repository.BookingRepository;
import com.commutesync.employee.repository.EmployeeRepository;
import com.commutesync.fleet.domain.DriverStatus;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.repository.DriverRepository;
import com.commutesync.fleet.repository.VehicleRepository;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.repository.TripRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    void dashboardAggregatesMetrics() {
        when(employeeRepository.count()).thenReturn(120L);
        when(driverRepository.count()).thenReturn(15L);
        when(driverRepository.countByStatusAndAvailableTrue(DriverStatus.ACTIVE)).thenReturn(11L);
        when(vehicleRepository.count()).thenReturn(20L);
        when(vehicleRepository.countByStatus(VehicleStatus.AVAILABLE)).thenReturn(8L);
        when(tripRepository.countByStatusIn(any())).thenReturn(3L);
        when(tripRepository.countByStatusInAndServiceDateGreaterThanEqual(any(), any(LocalDate.class)))
                .thenReturn(7L);
        when(bookingRepository.count()).thenReturn(500L);
        when(bookingRepository.countByStatus(BookingStatus.CONFIRMED)).thenReturn(410L);
        when(bookingRepository.countByStatus(BookingStatus.CANCELLED)).thenReturn(90L);

        DashboardResponse dashboard = adminDashboardService.getDashboard();

        assertThat(dashboard.totalEmployees()).isEqualTo(120);
        assertThat(dashboard.totalDrivers()).isEqualTo(15);
        assertThat(dashboard.availableDrivers()).isEqualTo(11);
        assertThat(dashboard.totalVehicles()).isEqualTo(20);
        assertThat(dashboard.availableVehicles()).isEqualTo(8);
        assertThat(dashboard.activeTrips()).isEqualTo(3);
        assertThat(dashboard.upcomingTrips()).isEqualTo(7);
        assertThat(dashboard.totalBookings()).isEqualTo(500);
        assertThat(dashboard.confirmedBookings()).isEqualTo(410);
        assertThat(dashboard.cancelledBookings()).isEqualTo(90);
        assertThat(dashboard.generatedAt()).isNotNull();
    }

    @Test
    void tripSummaryGroupsByStatusWithZeroDefaults() {
        when(tripRepository.countGroupedByStatus(null)).thenReturn(List.<Object[]>of(
                new Object[]{TripStatus.SCHEDULED, 3L},
                new Object[]{TripStatus.COMPLETED, 1L}));

        TripStatusSummaryResponse summary = adminDashboardService.getTripSummary(null);

        assertThat(summary.serviceDate()).isNull();
        assertThat(summary.total()).isEqualTo(4);
        assertThat(summary.byStatus()).containsEntry(TripStatus.SCHEDULED, 3L);
        assertThat(summary.byStatus()).containsEntry(TripStatus.COMPLETED, 1L);
        assertThat(summary.byStatus()).containsEntry(TripStatus.IN_PROGRESS, 0L);
        assertThat(summary.byStatus()).containsKeys(TripStatus.values());
    }

    @Test
    void tripSummaryAppliesServiceDateFilter() {
        LocalDate date = LocalDate.of(2026, 9, 20);
        when(tripRepository.countGroupedByStatus(date)).thenReturn(List.<Object[]>of(
                new Object[]{TripStatus.ASSIGNED, 2L}));

        TripStatusSummaryResponse summary = adminDashboardService.getTripSummary(date);

        assertThat(summary.serviceDate()).isEqualTo(date);
        assertThat(summary.total()).isEqualTo(2);
        assertThat(summary.byStatus()).containsEntry(TripStatus.ASSIGNED, 2L);
    }

    @Test
    void fleetSummaryAggregatesVehiclesAndDrivers() {
        when(vehicleRepository.count()).thenReturn(20L);
        when(vehicleRepository.countByStatus(VehicleStatus.AVAILABLE)).thenReturn(8L);
        when(vehicleRepository.countByStatus(VehicleStatus.IN_USE)).thenReturn(6L);
        when(vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE)).thenReturn(4L);
        when(vehicleRepository.countByStatus(VehicleStatus.INACTIVE)).thenReturn(2L);
        when(driverRepository.count()).thenReturn(15L);
        when(driverRepository.countByStatus(DriverStatus.ACTIVE)).thenReturn(12L);
        when(driverRepository.countByStatusAndAvailableTrue(DriverStatus.ACTIVE)).thenReturn(9L);

        FleetSummaryResponse summary = adminDashboardService.getFleetSummary();

        assertThat(summary.totalVehicles()).isEqualTo(20);
        assertThat(summary.availableVehicles()).isEqualTo(8);
        assertThat(summary.vehiclesInUse()).isEqualTo(6);
        assertThat(summary.vehiclesInMaintenance()).isEqualTo(4);
        assertThat(summary.vehiclesInactive()).isEqualTo(2);
        assertThat(summary.totalDrivers()).isEqualTo(15);
        assertThat(summary.activeDrivers()).isEqualTo(12);
        assertThat(summary.availableDrivers()).isEqualTo(9);
    }
}
