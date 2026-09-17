package com.commutesync.schedule.service;

import com.commutesync.common.api.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShiftService {

    private final ShiftRepository shiftRepository;
    private final ScheduleRepository scheduleRepository;
    private final EmployeeRepository employeeRepository;

    public ShiftService(ShiftRepository shiftRepository,
                        ScheduleRepository scheduleRepository,
                        EmployeeRepository employeeRepository) {
        this.shiftRepository = shiftRepository;
        this.scheduleRepository = scheduleRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public ShiftResponse create(CreateShiftRequest request) {
        String shiftCode = normalizeCode(request.shiftCode());
        if (shiftRepository.existsByShiftCode(shiftCode)) {
            throw new DuplicateResourceException("Shift code already exists: " + shiftCode);
        }
        ensureTimesDiffer(request.startTime(), request.endTime());

        Shift shift = new Shift();
        shift.setShiftCode(shiftCode);
        shift.setName(request.name().trim());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setStatus(request.status() == null ? ShiftStatus.ACTIVE : request.status());

        return ShiftResponse.from(shiftRepository.save(shift));
    }

    @Transactional(readOnly = true)
    public PageResponse<ShiftResponse> getAll(ShiftStatus status, String search, Pageable pageable) {
        String normalizedSearch = (search == null || search.isBlank()) ? null : search.trim();
        Page<Shift> page = shiftRepository.search(status, normalizedSearch, pageable);
        return PageResponse.from(page.map(ShiftResponse::from));
    }

    @Transactional(readOnly = true)
    public ShiftResponse getById(Long id) {
        return ShiftResponse.from(findById(id));
    }

    @Transactional
    public ShiftResponse update(Long id, UpdateShiftRequest request) {
        Shift shift = findById(id);
        String shiftCode = normalizeCode(request.shiftCode());

        if (!shift.getShiftCode().equals(shiftCode) && shiftRepository.existsByShiftCode(shiftCode)) {
            throw new DuplicateResourceException("Shift code already exists: " + shiftCode);
        }
        ensureTimesDiffer(request.startTime(), request.endTime());

        shift.setShiftCode(shiftCode);
        shift.setName(request.name().trim());
        shift.setStartTime(request.startTime());
        shift.setEndTime(request.endTime());
        shift.setStatus(request.status());

        return ShiftResponse.from(shiftRepository.save(shift));
    }

    @Transactional
    public void delete(Long id) {
        Shift shift = findById(id);
        if (scheduleRepository.existsByShiftId(id)) {
            throw new ResourceInUseException(
                    "Shift is used by a schedule and cannot be deleted");
        }
        if (employeeRepository.existsByShiftId(id)) {
            throw new ResourceInUseException(
                    "Shift is assigned to employees and cannot be deleted");
        }
        shiftRepository.delete(shift);
    }

    private Shift findById(Long id) {
        return shiftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Shift", id));
    }

    private void ensureTimesDiffer(LocalTime startTime, LocalTime endTime) {
        if (startTime.equals(endTime)) {
            throw new BusinessException("Shift start time and end time must be different");
        }
    }

    private String normalizeCode(String shiftCode) {
        return shiftCode.trim().toUpperCase();
    }
}
