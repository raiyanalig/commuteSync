package com.commutesync.employee.service;

import com.commutesync.common.api.PageResponse;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.domain.EmployeeStatus;
import com.commutesync.employee.dto.CreateEmployeeRequest;
import com.commutesync.employee.dto.EmployeeResponse;
import com.commutesync.employee.dto.UpdateEmployeeRequest;
import com.commutesync.employee.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public EmployeeResponse create(CreateEmployeeRequest request) {
        String employeeCode = normalizeCode(request.employeeCode());
        String email = normalizeEmail(request.email());

        if (employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new DuplicateResourceException("Employee code already exists: " + employeeCode);
        }
        if (employeeRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }

        Employee employee = new Employee();
        employee.setEmployeeCode(employeeCode);
        applyDetails(employee, request.fullName(), email, request.phone(),
                request.pickupAddress(), request.pickupPoint(), request.shiftId());
        employee.setStatus(request.status() == null ? EmployeeStatus.ACTIVE : request.status());

        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> getAll(EmployeeStatus status, String search, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        Page<Employee> page = employeeRepository.search(status, normalizedSearch, pageable);
        return PageResponse.from(page.map(EmployeeResponse::from));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return EmployeeResponse.from(findById(id));
    }

    @Transactional
    public EmployeeResponse update(Long id, UpdateEmployeeRequest request) {
        Employee employee = findById(id);
        String employeeCode = normalizeCode(request.employeeCode());
        String email = normalizeEmail(request.email());

        if (!employee.getEmployeeCode().equals(employeeCode)
                && employeeRepository.existsByEmployeeCode(employeeCode)) {
            throw new DuplicateResourceException("Employee code already exists: " + employeeCode);
        }
        if (!employee.getEmail().equals(email)
                && employeeRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already registered: " + email);
        }

        employee.setEmployeeCode(employeeCode);
        applyDetails(employee, request.fullName(), email, request.phone(),
                request.pickupAddress(), request.pickupPoint(), request.shiftId());
        employee.setStatus(request.status());

        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = findById(id);
        employeeRepository.delete(employee);
    }

    private Employee findById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));
    }

    private void applyDetails(Employee employee,
                              String fullName,
                              String email,
                              String phone,
                              String pickupAddress,
                              String pickupPoint,
                              Long shiftId) {
        employee.setFullName(fullName.trim());
        employee.setEmail(email);
        employee.setPhone(phone.trim());
        employee.setPickupAddress(pickupAddress.trim());
        employee.setPickupPoint((pickupPoint == null || pickupPoint.isBlank()) ? null : pickupPoint.trim());
        employee.setShiftId(shiftId);
    }

    private String normalizeCode(String employeeCode) {
        return employeeCode.trim().toUpperCase();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
