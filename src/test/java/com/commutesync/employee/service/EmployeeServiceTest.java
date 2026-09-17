package com.commutesync.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.domain.EmployeeStatus;
import com.commutesync.employee.dto.CreateEmployeeRequest;
import com.commutesync.employee.dto.EmployeeResponse;
import com.commutesync.employee.dto.UpdateEmployeeRequest;
import com.commutesync.employee.repository.EmployeeRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createNormalizesAndSavesEmployee() {
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(false);
        when(employeeRepository.existsByEmail("raiyan@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        EmployeeResponse response = employeeService.create(createRequest());

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.employeeCode()).isEqualTo("EMP001");
        assertThat(response.email()).isEqualTo("raiyan@example.com");
        assertThat(response.status()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void createRejectsDuplicateEmployeeCode() {
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(createRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("EMP001");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(employeeRepository.existsByEmployeeCode("EMP001")).thenReturn(false);
        when(employeeRepository.existsByEmail("raiyan@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(createRequest()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("raiyan@example.com");

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void getAllReturnsPagedResponse() {
        Employee employee = employee(1L, "EMP001", "raiyan@example.com");
        Page<Employee> page = new PageImpl<>(List.of(employee), PageRequest.of(0, 10), 1);
        when(employeeRepository.search(isNull(), isNull(), any(Pageable.class))).thenReturn(page);

        PageResponse<EmployeeResponse> result = employeeService.getAll(null, "   ", PageRequest.of(0, 10));

        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content().get(0).employeeCode()).isEqualTo("EMP001");
    }

    @Test
    void getByIdReturnsEmployee() {
        when(employeeRepository.findById(1L))
                .thenReturn(Optional.of(employee(1L, "EMP001", "raiyan@example.com")));

        EmployeeResponse response = employeeService.getById(1L);

        assertThat(response.employeeCode()).isEqualTo("EMP001");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateAppliesNewValues() {
        Employee existing = employee(1L, "EMP001", "raiyan@example.com");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmployeeCode("EMP002")).thenReturn(false);
        when(employeeRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "EMP002", "New Name", "New@Example.com", "+919876543210",
                "New Address", "Stop B", 9L, EmployeeStatus.ON_LEAVE);

        EmployeeResponse response = employeeService.update(1L, request);

        assertThat(response.employeeCode()).isEqualTo("EMP002");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.pickupPoint()).isEqualTo("Stop B");
        assertThat(response.shiftId()).isEqualTo(9L);
        assertThat(response.status()).isEqualTo(EmployeeStatus.ON_LEAVE);
    }

    @Test
    void updateRejectsDuplicateEmailWhenChanged() {
        Employee existing = employee(1L, "EMP001", "raiyan@example.com");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByEmail("taken@example.com")).thenReturn(true);

        UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                "EMP001", "Raiyan Ali", "taken@example.com", "+919876543210",
                "12 MG Road", null, null, EmployeeStatus.ACTIVE);

        assertThatThrownBy(() -> employeeService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(employeeRepository, never()).save(any(Employee.class));
    }

    @Test
    void deleteRemovesEmployee() {
        Employee existing = employee(1L, "EMP001", "raiyan@example.com");
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));

        employeeService.delete(1L);

        verify(employeeRepository).delete(existing);
    }

    @Test
    void deleteThrowsWhenMissing() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(employeeRepository, never()).delete(any(Employee.class));
    }

    private CreateEmployeeRequest createRequest() {
        return new CreateEmployeeRequest(
                "emp001", "Raiyan Ali", "Raiyan@Example.com", "+919876543210",
                "12 MG Road", "Stop A", 5L, null);
    }

    private Employee employee(Long id, String employeeCode, String email) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setEmployeeCode(employeeCode);
        employee.setFullName("Raiyan Ali");
        employee.setEmail(email);
        employee.setPhone("+919876543210");
        employee.setPickupAddress("12 MG Road");
        employee.setPickupPoint("Stop A");
        employee.setShiftId(5L);
        employee.setStatus(EmployeeStatus.ACTIVE);
        return employee;
    }
}
