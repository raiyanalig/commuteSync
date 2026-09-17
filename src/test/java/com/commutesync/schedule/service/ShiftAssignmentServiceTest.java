package com.commutesync.schedule.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.domain.EmployeeStatus;
import com.commutesync.employee.repository.EmployeeRepository;
import com.commutesync.schedule.domain.Shift;
import com.commutesync.schedule.domain.ShiftStatus;
import com.commutesync.schedule.dto.EmployeeShiftResponse;
import com.commutesync.schedule.repository.ShiftRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShiftAssignmentServiceTest {

    @Mock
    private ShiftRepository shiftRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private ShiftAssignmentService shiftAssignmentService;

    @Test
    void assignSetsEmployeeShift() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee(2L, null)));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeShiftResponse response = shiftAssignmentService.assignEmployee(1L, 2L);

        assertThat(response.shiftId()).isEqualTo(1L);
    }

    @Test
    void assignRejectsInactiveShift() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.INACTIVE)));

        assertThatThrownBy(() -> shiftAssignmentService.assignEmployee(1L, 2L))
                .isInstanceOf(BusinessException.class);

        verify(employeeRepository, never()).findById(any());
    }

    @Test
    void assignThrowsWhenShiftMissing() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftAssignmentService.assignEmployee(99L, 2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void assignThrowsWhenEmployeeMissing() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shiftAssignmentService.assignEmployee(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unassignClearsEmployeeShift() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee(2L, 1L)));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EmployeeShiftResponse response = shiftAssignmentService.unassignEmployee(1L, 2L);

        assertThat(response.shiftId()).isNull();
    }

    @Test
    void unassignRejectsWhenEmployeeNotOnShift() {
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift(1L, ShiftStatus.ACTIVE)));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(employee(2L, 5L)));

        assertThatThrownBy(() -> shiftAssignmentService.unassignEmployee(1L, 2L))
                .isInstanceOf(BusinessException.class);

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void getEmployeesThrowsWhenShiftMissing() {
        when(shiftRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> shiftAssignmentService.getEmployees(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getEmployeesReturnsAssignedEmployees() {
        when(shiftRepository.existsById(1L)).thenReturn(true);
        when(employeeRepository.findByShiftId(1L)).thenReturn(List.of(employee(2L, 1L)));

        List<EmployeeShiftResponse> employees = shiftAssignmentService.getEmployees(1L);

        assertThat(employees).hasSize(1);
        assertThat(employees.get(0).shiftId()).isEqualTo(1L);
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

    private Employee employee(Long id, Long shiftId) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setEmployeeCode("EMP00" + id);
        employee.setFullName("Employee " + id);
        employee.setEmail("employee" + id + "@example.com");
        employee.setPhone("+919876543210");
        employee.setPickupAddress("Address " + id);
        employee.setStatus(EmployeeStatus.ACTIVE);
        employee.setShiftId(shiftId);
        return employee;
    }
}
