package com.commutesync.schedule.repository;

import com.commutesync.schedule.domain.Shift;
import com.commutesync.schedule.domain.ShiftStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftRepository extends JpaRepository<Shift, Long> {

    boolean existsByShiftCode(String shiftCode);

    @Query("""
            SELECT s FROM Shift s
            WHERE (:status IS NULL OR s.status = :status)
              AND (:search IS NULL
                   OR LOWER(s.shiftCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Shift> search(@Param("status") ShiftStatus status,
                       @Param("search") String search,
                       Pageable pageable);
}
