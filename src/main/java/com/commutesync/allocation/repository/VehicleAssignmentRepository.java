package com.commutesync.allocation.repository;

import com.commutesync.allocation.domain.VehicleAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VehicleAssignmentRepository extends JpaRepository<VehicleAssignment, Long> {

    List<VehicleAssignment> findByScheduleId(Long scheduleId);

    boolean existsByScheduleIdAndVehicleId(Long scheduleId, Long vehicleId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM VehicleAssignment va WHERE va.schedule.id = :scheduleId")
    void deleteByScheduleId(@Param("scheduleId") Long scheduleId);
}
