package com.commutesync.schedule.service;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.repository.DriverRepository;
import com.commutesync.fleet.repository.VehicleRepository;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.repository.RouteRepository;
import com.commutesync.schedule.domain.Schedule;
import com.commutesync.schedule.domain.ScheduleStatus;
import com.commutesync.schedule.domain.Shift;
import com.commutesync.schedule.domain.ShiftStatus;
import com.commutesync.schedule.dto.CreateScheduleRequest;
import com.commutesync.schedule.dto.ScheduleResponse;
import com.commutesync.schedule.dto.UpdateScheduleRequest;
import com.commutesync.schedule.repository.ScheduleRepository;
import com.commutesync.schedule.repository.ShiftRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ShiftRepository shiftRepository;
    private final RouteRepository routeRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           ShiftRepository shiftRepository,
                           RouteRepository routeRepository,
                           VehicleRepository vehicleRepository,
                           DriverRepository driverRepository) {
        this.scheduleRepository = scheduleRepository;
        this.shiftRepository = shiftRepository;
        this.routeRepository = routeRepository;
        this.vehicleRepository = vehicleRepository;
        this.driverRepository = driverRepository;
    }

    @Transactional
    public ScheduleResponse create(CreateScheduleRequest request) {
        Shift shift = requireActiveShift(request.shiftId());
        Route route = requireActiveRoute(request.routeId());
        Vehicle vehicle = resolveVehicle(request.vehicleId());
        Driver driver = resolveDriver(request.driverId());
        validateArrival(request.departureTime(), request.arrivalTime());

        if (scheduleRepository.existsByServiceDateAndShiftIdAndRouteIdAndDepartureTime(
                request.serviceDate(), request.shiftId(), request.routeId(), request.departureTime())) {
            throw new DuplicateResourceException(
                    "A schedule already exists for this route, shift and departure time on "
                            + request.serviceDate());
        }

        Schedule schedule = new Schedule();
        schedule.setShift(shift);
        schedule.setRoute(route);
        schedule.setServiceDate(request.serviceDate());
        schedule.setDepartureTime(request.departureTime());
        schedule.setArrivalTime(request.arrivalTime());
        schedule.setVehicle(vehicle);
        schedule.setDriver(driver);
        schedule.setStatus(request.status() == null ? ScheduleStatus.ACTIVE : request.status());

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional(readOnly = true)
    public PageResponse<ScheduleResponse> getAll(LocalDate serviceDate, ScheduleStatus status,
                                                 Long routeId, Pageable pageable) {
        Page<Schedule> page = scheduleRepository.search(serviceDate, status, routeId, pageable);
        return PageResponse.from(page.map(ScheduleResponse::from));
    }

    @Transactional(readOnly = true)
    public ScheduleResponse getById(Long id) {
        return ScheduleResponse.from(findWithDetails(id));
    }

    @Transactional
    public ScheduleResponse update(Long id, UpdateScheduleRequest request) {
        Schedule schedule = findWithDetails(id);
        Shift shift = requireActiveShift(request.shiftId());
        Route route = requireActiveRoute(request.routeId());
        Vehicle vehicle = resolveVehicle(request.vehicleId());
        Driver driver = resolveDriver(request.driverId());
        validateArrival(request.departureTime(), request.arrivalTime());

        boolean slotChanged = !schedule.getServiceDate().equals(request.serviceDate())
                || !schedule.getShift().getId().equals(request.shiftId())
                || !schedule.getRoute().getId().equals(request.routeId())
                || !schedule.getDepartureTime().equals(request.departureTime());

        if (slotChanged && scheduleRepository.existsByServiceDateAndShiftIdAndRouteIdAndDepartureTime(
                request.serviceDate(), request.shiftId(), request.routeId(), request.departureTime())) {
            throw new DuplicateResourceException(
                    "A schedule already exists for this route, shift and departure time on "
                            + request.serviceDate());
        }

        schedule.setShift(shift);
        schedule.setRoute(route);
        schedule.setServiceDate(request.serviceDate());
        schedule.setDepartureTime(request.departureTime());
        schedule.setArrivalTime(request.arrivalTime());
        schedule.setVehicle(vehicle);
        schedule.setDriver(driver);
        schedule.setStatus(request.status());

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public ScheduleResponse updateStatus(Long id, ScheduleStatus status) {
        Schedule schedule = findWithDetails(id);
        schedule.setStatus(status);
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public void delete(Long id) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", id));
        scheduleRepository.delete(schedule);
    }

    private Shift requireActiveShift(Long shiftId) {
        Shift shift = shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));
        if (shift.getStatus() != ShiftStatus.ACTIVE) {
            throw new BusinessException("Shift is not active: " + shift.getShiftCode());
        }
        return shift;
    }

    private Route requireActiveRoute(Long routeId) {
        Route route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route", routeId));
        if (route.getStatus() != RouteStatus.ACTIVE) {
            throw new BusinessException("Route is not active: " + route.getRouteCode());
        }
        return route;
    }

    private Vehicle resolveVehicle(Long vehicleId) {
        if (vehicleId == null) {
            return null;
        }
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", vehicleId));
        if (!vehicle.isAssignable()) {
            throw new BusinessException("Vehicle is not available for scheduling (status: "
                    + vehicle.getStatus() + ")");
        }
        return vehicle;
    }

    private Driver resolveDriver(Long driverId) {
        if (driverId == null) {
            return null;
        }
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", driverId));
        if (!driver.isAssignable()) {
            throw new BusinessException("Driver is not available for scheduling (status: "
                    + driver.getStatus() + ", available: " + driver.isAvailable() + ")");
        }
        return driver;
    }

    private void validateArrival(LocalTime departureTime, LocalTime arrivalTime) {
        if (arrivalTime != null && arrivalTime.equals(departureTime)) {
            throw new BusinessException("Arrival time must differ from departure time");
        }
    }

    private Schedule findWithDetails(Long id) {
        return scheduleRepository.findWithDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule", id));
    }
}
