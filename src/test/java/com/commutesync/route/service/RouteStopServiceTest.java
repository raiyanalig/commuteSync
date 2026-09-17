package com.commutesync.route.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.commutesync.common.exception.BusinessException;
import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.domain.Stop;
import com.commutesync.route.dto.CreateStopRequest;
import com.commutesync.route.dto.RouteResponse;
import com.commutesync.route.dto.StopResponse;
import com.commutesync.route.dto.UpdateStopRequest;
import com.commutesync.route.repository.RouteRepository;
import com.commutesync.route.repository.StopRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteStopServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private StopRepository stopRepository;

    @InjectMocks
    private RouteStopService routeStopService;

    @Test
    void getStopsReturnsOrderedStops() {
        when(routeRepository.existsById(1L)).thenReturn(true);
        when(stopRepository.findByRouteIdOrderByStopOrderAsc(1L))
                .thenReturn(List.of(stop(10L, "A", 1), stop(11L, "B", 2)));

        List<StopResponse> stops = routeStopService.getStops(1L);

        assertThat(stops).hasSize(2);
        assertThat(stops.get(0).stopOrder()).isEqualTo(1);
    }

    @Test
    void getStopsThrowsWhenRouteMissing() {
        when(routeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> routeStopService.getStops(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addStopAppendsAtEnd() {
        Route route = routeWithStops(1L, stop(10L, "A", 1), stop(11L, "B", 2));
        when(routeRepository.findWithStopsById(1L)).thenReturn(Optional.of(route));
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RouteResponse response = routeStopService.addStop(1L, new CreateStopRequest("C", "Addr C"));

        assertThat(response.stops()).hasSize(3);
        assertThat(response.stops().get(2).name()).isEqualTo("C");
        assertThat(response.stops().get(2).stopOrder()).isEqualTo(3);
    }

    @Test
    void addStopThrowsWhenRouteMissing() {
        when(routeRepository.findWithStopsById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeStopService.addStop(99L, new CreateStopRequest("C", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStopChangesFields() {
        Route route = routeWithStops(1L, stop(10L, "A", 1));
        when(routeRepository.findWithStopsById(1L)).thenReturn(Optional.of(route));

        StopResponse response = routeStopService.updateStop(1L, 10L, new UpdateStopRequest("A2", "Addr 2"));

        assertThat(response.name()).isEqualTo("A2");
        assertThat(response.address()).isEqualTo("Addr 2");
    }

    @Test
    void updateStopThrowsWhenStopNotInRoute() {
        Route route = routeWithStops(1L, stop(10L, "A", 1));
        when(routeRepository.findWithStopsById(1L)).thenReturn(Optional.of(route));

        assertThatThrownBy(() -> routeStopService.updateStop(1L, 99L, new UpdateStopRequest("X", null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeStopRenumbersRemaining() {
        Route route = routeWithStops(1L, stop(10L, "A", 1), stop(11L, "B", 2), stop(12L, "C", 3));
        when(routeRepository.findWithStopsById(1L)).thenReturn(Optional.of(route));

        routeStopService.removeStop(1L, 11L);

        assertThat(route.getStops()).hasSize(2);
        assertThat(route.getStops()).extracting(Stop::getStopOrder).containsExactlyInAnyOrder(1, 2);
        verify(stopRepository).flush();
    }

    @Test
    void reorderStopsReassignsOrder() {
        Route route = routeWithStops(1L, stop(10L, "A", 1), stop(11L, "B", 2), stop(12L, "C", 3));
        when(routeRepository.findWithStopsById(1L)).thenReturn(Optional.of(route));

        RouteResponse response = routeStopService.reorderStops(1L, List.of(12L, 10L, 11L));

        assertThat(response.stops()).extracting(StopResponse::id).containsExactly(12L, 10L, 11L);
        assertThat(response.stops()).extracting(StopResponse::stopOrder).containsExactly(1, 2, 3);
        verify(stopRepository).flush();
    }

    @Test
    void reorderRejectsWrongNumberOfStops() {
        Route route = routeWithStops(1L, stop(10L, "A", 1), stop(11L, "B", 2));
        when(routeRepository.findWithStopsById(1L)).thenReturn(Optional.of(route));

        assertThatThrownBy(() -> routeStopService.reorderStops(1L, List.of(10L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exactly 2 stop ids");

        verify(stopRepository, never()).flush();
    }

    @Test
    void reorderRejectsUnknownStopIds() {
        Route route = routeWithStops(1L, stop(10L, "A", 1), stop(11L, "B", 2));
        when(routeRepository.findWithStopsById(1L)).thenReturn(Optional.of(route));

        assertThatThrownBy(() -> routeStopService.reorderStops(1L, List.of(10L, 99L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("unknown or duplicate");

        verify(stopRepository, never()).flush();
    }

    private Route routeWithStops(Long routeId, Stop... stops) {
        Route route = new Route();
        route.setId(routeId);
        route.setRouteCode("R-01");
        route.setName("Morning Route");
        route.setSource("Hostel");
        route.setDestination("Office");
        route.setStatus(RouteStatus.ACTIVE);
        for (Stop stop : stops) {
            route.addStop(stop);
        }
        return route;
    }

    private Stop stop(Long id, String name, int stopOrder) {
        Stop stop = new Stop();
        stop.setId(id);
        stop.setName(name);
        stop.setStopOrder(stopOrder);
        return stop;
    }
}
