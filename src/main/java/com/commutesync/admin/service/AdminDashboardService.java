package com.commutesync.admin.service;

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
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only operational metrics. It only composes existing repository count queries,
 * so no booking/trip business logic is duplicated here.
 */
@Service
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final Collection<TripStatus> ACTIVE_TRIP_STATUSES =
            EnumSet.of(TripStatus.STARTED, TripStatus.IN_PROGRESS);
    private static final Collection<TripStatus> UPCOMING_TRIP_STATUSES =
            EnumSet.of(TripStatus.SCHEDULED, TripStatus.ASSIGNED);

    private final EmployeeRepository employeeRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final TripRepository tripRepository;
    private final BookingRepository bookingRepository;

    public AdminDashboardService(EmployeeRepository employeeRepository,
                                 DriverRepository driverRepository,
                                 VehicleRepository vehicleRepository,
                                 TripRepository tripRepository,
                                 BookingRepository bookingRepository) {
        this.employeeRepository = employeeRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.tripRepository = tripRepository;
        this.bookingRepository = bookingRepository;
    }

    public DashboardResponse getDashboard() {
        return new DashboardResponse(
                employeeRepository.count(),
                driverRepository.count(),
                driverRepository.countByStatusAndAvailableTrue(DriverStatus.ACTIVE),
                vehicleRepository.count(),
                vehicleRepository.countByStatus(VehicleStatus.AVAILABLE),
                tripRepository.countByStatusIn(ACTIVE_TRIP_STATUSES),
                tripRepository.countByStatusInAndServiceDateGreaterThanEqual(
                        UPCOMING_TRIP_STATUSES, LocalDate.now()),
                bookingRepository.count(),
                bookingRepository.countByStatus(BookingStatus.CONFIRMED),
                bookingRepository.countByStatus(BookingStatus.CANCELLED),
                Instant.now()
        );
    }

    public TripStatusSummaryResponse getTripSummary(LocalDate serviceDate) {
        Map<TripStatus, Long> byStatus = new LinkedHashMap<>();
        for (TripStatus status : TripStatus.values()) {
            byStatus.put(status, 0L);
        }
        for (Object[] row : tripRepository.countGroupedByStatus(serviceDate)) {
            byStatus.put((TripStatus) row[0], (Long) row[1]);
        }
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        return new TripStatusSummaryResponse(serviceDate, total, byStatus);
    }

    public FleetSummaryResponse getFleetSummary() {
        return new FleetSummaryResponse(
                vehicleRepository.count(),
                vehicleRepository.countByStatus(VehicleStatus.AVAILABLE),
                vehicleRepository.countByStatus(VehicleStatus.IN_USE),
                vehicleRepository.countByStatus(VehicleStatus.MAINTENANCE),
                vehicleRepository.countByStatus(VehicleStatus.INACTIVE),
                driverRepository.count(),
                driverRepository.countByStatus(DriverStatus.ACTIVE),
                driverRepository.countByStatusAndAvailableTrue(DriverStatus.ACTIVE)
        );
    }
}
