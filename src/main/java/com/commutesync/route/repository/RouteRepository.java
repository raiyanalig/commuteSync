package com.commutesync.route.repository;

import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RouteRepository extends JpaRepository<Route, Long> {

    boolean existsByRouteCode(String routeCode);

    @EntityGraph(attributePaths = "stops")
    @Query("SELECT r FROM Route r WHERE r.id = :id")
    Optional<Route> findWithStopsById(@Param("id") Long id);

    @Query("""
            SELECT r FROM Route r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:search IS NULL
                   OR LOWER(r.routeCode) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.source) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(r.destination) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Route> search(@Param("status") RouteStatus status,
                       @Param("search") String search,
                       Pageable pageable);
}
