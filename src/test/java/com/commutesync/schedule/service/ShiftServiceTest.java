package com.commutesync.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceInUseException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.employee.repository.EmployeeRepository;
import com.commutesync.schedule.domain.Shift;
import com.commutesync.schedule.domain.ShiftStatus;
import com.commutesync.schedule.dto.CreateShiftRequest;
import com.commutesync.schedule.dto.ShiftResponse;
import com.commutesync.schedule.dto.UpdateShiftRequest;
import com.commutesync.schedule.repository.ScheduleRepository;
import com.commutesync.schedule.repository.ShiftRepository;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftServiceTest {

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private ShiftService shiftService;

    @Test
    void createNormalizesCodeAndDetectsMidnightCrossing() {
        when(shiftRepository.existsByShiftCode("NIGHT")).thenReturn(false);
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> {
            Shift saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ShiftResponse response = shiftService.create(new CreateShiftRequest(
                "night", "Night Shift", LocalTime.of(22, 0), LocalTime.of(6, 0), null));

        assertThat(response.shiftCode()).isEqualTo("NIGHT");
        assertThat(response.crossesMidnight()).isTrue();
        assertThat(response.status()).isEqualTo(ShiftStatus.ACTIVE);
    }

    @Test
    void createRejectsDuplicateCode() {
        when(shiftRepository.existsByShiftCode("MORNING")).thenReturn(true);

        assertThatThrownBy(() -> shiftService.create(new CreateShiftRequest(
                "MORNING", "Morning", LocalTime.of(9, 0), LocalTime.of(18, 0), null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(shiftRepository, never()).save(any(Shift.class));
    }

    @Test
    void createRejectsEqualStartAndEnd() {
        when(shiftRepository.existsByShiftCode("MORNING")).thenReturn(false);

        assertThatThrownBy(() -> shiftService.create(new CreateShiftRequest(
                "MORNING", "Morning", LocalTime.of(9, 0), LocalTime.of(9, 0), null)))
                .isInstanceOf(BusinessException.class);

        verify(shiftRepository, never()).save(any(Shift.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChanges() {
        Shift existing = shift(1L, ShiftStatus.ACTIVE);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(shiftRepository.existsByShiftCode("EVENING")).thenReturn(false);
        when(shiftRepository.save(any(Shift.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShiftResponse response = shiftService.update(1L, new UpdateShiftRequest(
                "evening", "Evening Shift", LocalTime.of(18, 0), LocalTime.of(23, 0),
                ShiftStatus.INACTIVE));

        assertThat(response.shiftCode()).isEqualTo("EVENING");
        assertThat(response.status()).isEqualTo(ShiftStatus.INACTIVE);
    }

    @Test
    void deleteRejectsWhenUsedBySchedule() {
        Shift existing = shift(1L, ShiftStatus.ACTIVE);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.existsByShiftId(1L)).thenReturn(true);

        assertThatThrownBy(() -> shiftService.delete(1L))
                .isInstanceOf(ResourceInUseException.class);

        verify(shiftRepository, never()).delete(any(Shift.class));
    }

    @Test
    void deleteRejectsWhenAssignedToEmployees() {
        Shift existing = shift(1L, ShiftStatus.ACTIVE);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.existsByShiftId(1L)).thenReturn(false);
        when(employeeRepository.existsByShiftId(1L)).thenReturn(true);

        assertThatThrownBy(() -> shiftService.delete(1L))
                .isInstanceOf(ResourceInUseException.class);

        verify(shiftRepository, never()).delete(any(Shift.class));
    }

    @Test
    void deleteRemovesWhenUnused() {
        Shift existing = shift(1L, ShiftStatus.ACTIVE);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(scheduleRepository.existsByShiftId(1L)).thenReturn(false);
        when(employeeRepository.existsByShiftId(1L)).thenReturn(false);

        shiftService.delete(1L);

        verify(shiftRepository).delete(existing);
    }

    private Shift shift(Long id, ShiftStatus status) {
        Shift shift = new Shift();
        shift.setId(id);
        shift.setShiftCode("MORNING");
        shift.setName("Morning Shift");
        shift.setStartTime(LocalTime.of(9, 0));
        shift.setEndTime(LocalTime.of(18, 0));
        shift.setStatus(status);
        return shift;
    }
}
