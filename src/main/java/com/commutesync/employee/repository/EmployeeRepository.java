package com.commutesync.employee.repository;

import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.domain.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);

    @Query("""
            SELECT e FROM Employee e
            WHERE (:status IS NULL OR e.status = :status)
              AND (:search IS NULL
                   OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.email) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(e.employeeCode) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Employee> search(@Param("status") EmployeeStatus status,
                          @Param("search") String search,
                          Pageable pageable);
}
