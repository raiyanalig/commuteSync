package com.commutesync.allocation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.allocation.algorithm.GreedyVehicleAllocator;
import com.commutesync.allocation.domain.VehicleAssignment;
import com.commutesync.allocation.dto.AllocationRequest;
import com.commutesync.allocation.dto.AllocationResponse;
import com.commutesync.allocation.dto.VehicleAssignmentResponse;
import com.commutesync.allocation.repository.VehicleAssignmentRepository;
import com.commutesync.booking.domain.BookingStatus;
import com.commutesync.booking.repository.BookingRepository;
import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import com.commutesync.fleet.repository.VehicleRepository;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.schedule.domain.Schedule;
import com.commutesync.schedule.domain.ScheduleStatus;
import com.commutesync.schedule.repository.ScheduleRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AllocationServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private VehicleAssignmentRepository assignmentRepository;

    private AllocationService allocationService;

    @BeforeEach
    void setUp() {
        allocationService = new AllocationService(
                new GreedyVehicleAllocator(), scheduleRepository, vehicleRepository,
                bookingRepository, assignmentRepository);
    }

    @Test
    void allocatePersistsAssignmentsFromDerivedDemand() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule(1L, ScheduleStatus.ACTIVE)));
        when(bookingRepository.countConfirmedByScheduleId(1L, BookingStatus.CONFIRMED)).thenReturn(20L);
        when(vehicleRepository.findByStatusAndCapacityGreaterThanOrderByCapacityDesc(
                VehicleStatus.AVAILABLE, 0))
                .thenReturn(List.of(vehicle(10L, "A", 12), vehicle(11L, "B", 12), vehicle(12L, "C", 12)));
        when(assignmentRepository.save(any(VehicleAssignment.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        AllocationResponse response = allocationService.allocate(new AllocationRequest(1L, null));

        assertThat(response.demand()).isEqualTo(20);
        assertThat(response.allocatedSeats()).isEqualTo(20);
        assertThat(response.fullyAllocated()).isTrue();
        assertThat(response.assignments()).hasSize(2);
        assertThat(response.assignments()).extracting(VehicleAssignmentResponse::passengerCount)
                .containsExactly(12, 8);
        verify(assignmentRepository).deleteByScheduleId(1L);
    }

    @Test
    void allocateUsesExplicitPassengerCount() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule(1L, ScheduleStatus.ACTIVE)));
        when(vehicleRepository.findByStatusAndCapacityGreaterThanOrderByCapacityDesc(
                VehicleStatus.AVAILABLE, 0))
                .thenReturn(List.of(vehicle(10L, "A", 12)));
        when(assignmentRepository.save(any(VehicleAssignment.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        AllocationResponse response = allocationService.allocate(new AllocationRequest(1L, 5));

        assertThat(response.demand()).isEqualTo(5);
        assertThat(response.assignments()).hasSize(1);
        assertThat(response.assignments().get(0).passengerCount()).isEqualTo(5);
        verify(bookingRepository, never()).countConfirmedByScheduleId(any(), any());
    }

    @Test
    void allocateReportsShortfallWhenCapacityInsufficient() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule(1L, ScheduleStatus.ACTIVE)));
        when(bookingRepository.countConfirmedByScheduleId(1L, BookingStatus.CONFIRMED)).thenReturn(30L);
        when(vehicleRepository.findByStatusAndCapacityGreaterThanOrderByCapacityDesc(
                VehicleStatus.AVAILABLE, 0))
                .thenReturn(List.of(vehicle(10L, "A", 12), vehicle(11L, "B", 12)));
        when(assignmentRepository.save(any(VehicleAssignment.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        AllocationResponse response = allocationService.allocate(new AllocationRequest(1L, null));

        assertThat(response.allocatedSeats()).isEqualTo(24);
        assertThat(response.unallocatedDemand()).isEqualTo(6);
        assertThat(response.fullyAllocated()).isFalse();
        assertThat(response.assignments()).hasSize(2);
    }

    @Test
    void allocateWithNoVehiclesReportsFullShortfall() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule(1L, ScheduleStatus.ACTIVE)));
        when(bookingRepository.countConfirmedByScheduleId(1L, BookingStatus.CONFIRMED)).thenReturn(15L);
        when(vehicleRepository.findByStatusAndCapacityGreaterThanOrderByCapacityDesc(
                VehicleStatus.AVAILABLE, 0))
                .thenReturn(List.of());

        AllocationResponse response = allocationService.allocate(new AllocationRequest(1L, null));

        assertThat(response.assignments()).isEmpty();
        assertThat(response.allocatedSeats()).isZero();
        assertThat(response.unallocatedDemand()).isEqualTo(15);
        assertThat(response.fullyAllocated()).isFalse();
        verify(assignmentRepository, never()).save(any(VehicleAssignment.class));
    }

    @Test
    void allocateRejectsInactiveSchedule() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule(1L, ScheduleStatus.INACTIVE)));

        assertThatThrownBy(() -> allocationService.allocate(new AllocationRequest(1L, 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");

        verify(assignmentRepository, never()).deleteByScheduleId(any());
    }

    @Test
    void allocateThrowsWhenScheduleMissing() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> allocationService.allocate(new AllocationRequest(99L, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void allocateReplacesPreviousPlan() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule(1L, ScheduleStatus.ACTIVE)));
        when(vehicleRepository.findByStatusAndCapacityGreaterThanOrderByCapacityDesc(
                VehicleStatus.AVAILABLE, 0))
                .thenReturn(List.of(vehicle(10L, "A", 12)));
        when(assignmentRepository.save(any(VehicleAssignment.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));

        allocationService.allocate(new AllocationRequest(1L, 5));

        verify(assignmentRepository).deleteByScheduleId(1L);
    }

    @Test
    void getAssignmentsReturnsList() {
        VehicleAssignment assignment = new VehicleAssignment();
        assignment.setId(50L);
        assignment.setSchedule(schedule(1L, ScheduleStatus.ACTIVE));
        assignment.setVehicle(vehicle(10L, "A", 12));
        assignment.setPassengerCount(8);
        assignment.setAllocatedCapacity(12);

        when(scheduleRepository.existsById(1L)).thenReturn(true);
        when(assignmentRepository.findByScheduleId(1L)).thenReturn(List.of(assignment));

        List<VehicleAssignmentResponse> response = allocationService.getAssignments(1L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).registrationNumber()).isEqualTo("A");
        assertThat(response.get(0).passengerCount()).isEqualTo(8);
    }

    @Test
    void getAssignmentsThrowsWhenScheduleMissing() {
        when(scheduleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> allocationService.getAssignments(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private final AtomicLong idSequence = new AtomicLong(100);

    private VehicleAssignment withId(VehicleAssignment assignment) {
        assignment.setId(idSequence.getAndIncrement());
        return assignment;
    }

    private Schedule schedule(Long id, ScheduleStatus status) {
        Route route = new Route();
        route.setId(2L);
        route.setRouteCode("R-01");
        route.setName("Morning Route");
        route.setSource("Hostel");
        route.setDestination("Office");
        route.setStatus(RouteStatus.ACTIVE);

        Schedule schedule = new Schedule();
        schedule.setId(id);
        schedule.setRoute(route);
        schedule.setServiceDate(LocalDate.now().plusDays(1));
        schedule.setDepartureTime(LocalTime.of(9, 0));
        schedule.setStatus(status);
        return schedule;
    }

    private Vehicle vehicle(Long id, String registrationNumber, int capacity) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber(registrationNumber);
        vehicle.setType(VehicleType.VAN);
        vehicle.setCapacity(capacity);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        return vehicle;
    }
}
