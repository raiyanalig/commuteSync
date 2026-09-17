package com.commutesync.schedule.service;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.repository.EmployeeRepository;
import com.commutesync.schedule.domain.Shift;
import com.commutesync.schedule.domain.ShiftStatus;
import com.commutesync.schedule.dto.EmployeeShiftResponse;
import com.commutesync.schedule.repository.ShiftRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftAssignmentService {

    private final ShiftRepository shiftRepository;
    private final EmployeeRepository employeeRepository;

    public ShiftAssignmentService(ShiftRepository shiftRepository,
                                  EmployeeRepository employeeRepository) {
        this.shiftRepository = shiftRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public EmployeeShiftResponse assignEmployee(Long shiftId, Long employeeId) {
        Shift shift = findShift(shiftId);
        if (shift.getStatus() != ShiftStatus.ACTIVE) {
            throw new BusinessException("Cannot assign employees to an inactive shift");
        }
        Employee employee = findEmployee(employeeId);
        employee.setShiftId(shiftId);
        return EmployeeShiftResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeShiftResponse unassignEmployee(Long shiftId, Long employeeId) {
        findShift(shiftId);
        Employee employee = findEmployee(employeeId);
        if (!shiftId.equals(employee.getShiftId())) {
            throw new BusinessException("Employee is not assigned to this shift");
        }
        employee.setShiftId(null);
        return EmployeeShiftResponse.from(employeeRepository.save(employee));
    }

    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getEmployees(Long shiftId) {
        if (!shiftRepository.existsById(shiftId)) {
            throw new ResourceNotFoundException("Shift", shiftId);
        }
        return employeeRepository.findByShiftId(shiftId).stream()
                .map(EmployeeShiftResponse::from)
                .toList();
    }

    private Shift findShift(Long shiftId) {
        return shiftRepository.findById(shiftId)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", shiftId));
    }

    private Employee findEmployee(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));
    }
}
