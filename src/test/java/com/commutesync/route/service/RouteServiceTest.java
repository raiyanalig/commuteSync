package com.commutesync.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.DuplicateResourceException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.dto.CreateRouteRequest;
import com.commutesync.route.dto.CreateStopRequest;
import com.commutesync.route.dto.RouteResponse;
import com.commutesync.route.dto.UpdateRouteRequest;
import com.commutesync.route.repository.RouteRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private RouteService routeService;

    @Test
    void createWithStopsAssignsSequentialOrder() {
        when(routeRepository.existsByRouteCode("R-01")).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        CreateRouteRequest request = new CreateRouteRequest(
                "r-01", "Morning Route", "Hostel", "Office",
                new BigDecimal("12.50"), 45, null,
                List.of(new CreateStopRequest("Stop A", "Addr A"),
                        new CreateStopRequest("Stop B", null)));

        RouteResponse response = routeService.create(request);

        assertThat(response.routeCode()).isEqualTo("R-01");
        assertThat(response.status()).isEqualTo(RouteStatus.ACTIVE);
        assertThat(response.stops()).hasSize(2);
        assertThat(response.stops().get(0).stopOrder()).isEqualTo(1);
        assertThat(response.stops().get(1).stopOrder()).isEqualTo(2);
        assertThat(response.stops().get(1).address()).isNull();
    }

    @Test
    void createRejectsDuplicateCode() {
        when(routeRepository.existsByRouteCode("R-01")).thenReturn(true);

        assertThatThrownBy(() -> routeService.create(new CreateRouteRequest(
                "R-01", "Morning Route", "Hostel", "Office", null, null, null, null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    void createRejectsSameSourceAndDestination() {
        when(routeRepository.existsByRouteCode("R-01")).thenReturn(false);

        assertThatThrownBy(() -> routeService.create(new CreateRouteRequest(
                "R-01", "Loop", "Office", "office", null, null, null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Source and destination must be different");

        verify(routeRepository, never()).save(any(Route.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(routeRepository.findWithStopsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateAppliesChanges() {
        Route existing = route(1L, "R-01");
        when(routeRepository.findWithStopsById(1L)).thenReturn(Optional.of(existing));
        when(routeRepository.existsByRouteCode("R-02")).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse response = routeService.update(1L, new UpdateRouteRequest(
                "r-02", "Evening Route", "Office", "Hostel",
                new BigDecimal("10.00"), 40, RouteStatus.INACTIVE));

        assertThat(response.routeCode()).isEqualTo("R-02");
        assertThat(response.source()).isEqualTo("Office");
        assertThat(response.destination()).isEqualTo("Hostel");
        assertThat(response.status()).isEqualTo(RouteStatus.INACTIVE);
    }

    @Test
    void deleteRemovesRoute() {
        Route existing = route(1L, "R-01");
        when(routeRepository.findById(1L)).thenReturn(Optional.of(existing));

        routeService.delete(1L);

        verify(routeRepository).delete(existing);
    }

    private Route route(Long id, String routeCode) {
        Route route = new Route();
        route.setId(id);
        route.setRouteCode(routeCode);
        route.setName("Morning Route");
        route.setSource("Hostel");
        route.setDestination("Office");
        route.setStatus(RouteStatus.ACTIVE);
        return route;
    }
}
