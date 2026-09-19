package com.commutesync.it;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commutesync.fleet.domain.Driver;
import com.commutesync.fleet.domain.DriverStatus;
import com.commutesync.fleet.domain.Vehicle;
import com.commutesync.fleet.domain.VehicleStatus;
import com.commutesync.fleet.domain.VehicleType;
import com.commutesync.fleet.repository.DriverRepository;
import com.commutesync.fleet.repository.VehicleRepository;
import com.commutesync.route.domain.Route;
import com.commutesync.route.domain.RouteStatus;
import com.commutesync.route.repository.RouteRepository;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class TripLifecycleIT extends IntegrationTestSupport {

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    private Long routeId;
    private Long vehicleId;
    private Long driverId;
    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = registerAndGetToken(uniqueEmail("admin"), "ADMIN");

        Route route = new Route();
        route.setRouteCode(uniqueCode("R"));
        route.setName("IT Route");
        route.setSource("Hostel");
        route.setDestination("Office");
        route.setStatus(RouteStatus.ACTIVE);
        routeId = routeRepository.save(route).getId();

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber(uniqueCode("PB"));
        vehicle.setType(VehicleType.VAN);
        vehicle.setCapacity(12);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicleId = vehicleRepository.save(vehicle).getId();

        Driver driver = new Driver();
        driver.setFullName("IT Driver");
        driver.setPhone("+919876543210");
        driver.setEmail(uniqueEmail("driver"));
        driver.setLicenseNumber(uniqueCode("DL"));
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(2));
        driver.setStatus(DriverStatus.ACTIVE);
        driver.setAvailable(true);
        driverId = driverRepository.save(driver).getId();
    }

    @Test
    void tripRunsThroughFullLifecycle() throws Exception {
        long tripId = createTrip();

        assignDriverAndVehicle(tripId);

        mockMvc.perform(post("/api/trips/{id}/start", tripId).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STARTED"));

        mockMvc.perform(post("/api/trips/{id}/progress", tripId).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(post("/api/trips/{id}/complete", tripId).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/trips/{id}", tripId).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void invalidTransitionIsRejected() throws Exception {
        long tripId = createTrip();
        assignDriverAndVehicle(tripId);

        mockMvc.perform(post("/api/trips/{id}/cancel", tripId).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/trips/{id}/start", tripId).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid trip state transition")));
    }

    @Test
    void assignedTripCanBeCancelled() throws Exception {
        long tripId = createTrip();
        assignDriverAndVehicle(tripId);

        String body = objectMapper.writeValueAsString(Map.of("reason", "Vehicle breakdown"));
        mockMvc.perform(post("/api/trips/{id}/cancel", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancellationReason").value("Vehicle breakdown"));
    }

    @Test
    void unknownTripReturns404() throws Exception {
        mockMvc.perform(get("/api/trips/{id}", 999_999L).header(HttpHeaders.AUTHORIZATION, bearer()))
                .andExpect(status().isNotFound());
    }

    private long createTrip() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "routeId", routeId,
                "serviceDate", LocalDate.now().plusDays(1).toString(),
                "departureTime", "09:15:00"));

        String response = mockMvc.perform(post("/api/trips")
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(response).get("id").asLong();
    }

    private void assignDriverAndVehicle(long tripId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("driverId", driverId, "vehicleId", vehicleId));
        mockMvc.perform(put("/api/trips/{id}/assignment", tripId)
                        .header(HttpHeaders.AUTHORIZATION, bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    private String bearer() {
        return "Bearer " + adminToken;
    }
}
