package com.commutesync.route.repository;

import com.commutesync.route.domain.Stop;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StopRepository extends JpaRepository<Stop, Long> {

    List<Stop> findByRouteIdOrderByStopOrderAsc(Long routeId);
}
