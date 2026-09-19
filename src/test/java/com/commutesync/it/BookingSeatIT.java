package com.commutesync.it;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commutesync.employee.domain.Employee;
import com.commutesync.employee.domain.EmployeeStatus;
import com.commutesync.employee.repository.EmployeeRepository;
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
import com.commutesync.trip.domain.Trip;
import com.commutesync.trip.domain.TripStatus;
import com.commutesync.trip.repository.TripRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

class BookingSeatIT extends IntegrationTestSupport {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private DriverRepository driverRepository;

    @Autowired
    private TripRepository tripRepository;

    private Long tripId;

    @BeforeEach
    void setUp() {
        Route route = new Route();
        route.setRouteCode(uniqueCode("R"));
        route.setName("IT Booking Route");
        route.setSource("Hostel");
        route.setDestination("Office");
        route.setStatus(RouteStatus.ACTIVE);
        route = routeRepository.save(route);

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber(uniqueCode("PB"));
        vehicle.setType(VehicleType.VAN);
        vehicle.setCapacity(2);
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        vehicle = vehicleRepository.save(vehicle);

        Driver driver = new Driver();
        driver.setFullName("IT Driver");
        driver.setPhone("+919876543210");
        driver.setEmail(uniqueEmail("driver"));
        driver.setLicenseNumber(uniqueCode("DL"));
        driver.setLicenseExpiryDate(LocalDate.now().plusYears(2));
        driver.setStatus(DriverStatus.ACTIVE);
        driver.setAvailable(true);
        driver = driverRepository.save(driver);

        Trip trip = new Trip();
        trip.setRoute(route);
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        trip.setServiceDate(LocalDate.now().plusDays(1));
        trip.setDepartureTime(LocalTime.of(9, 0));
        trip.setStatus(TripStatus.ASSIGNED);
        tripId = tripRepository.save(trip).getId();
    }

    @Test
    void bookingAssignsSeatsAndEnforcesCapacity() throws Exception {
        String firstToken = createEmployeeWithUser();
        String secondToken = createEmployeeWithUser();
        String thirdToken = createEmployeeWithUser();

        book(firstToken, tripId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.seatNumber").value(1));

        book(firstToken, tripId)
                .andExpect(status().isConflict());

        book(secondToken, tripId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatNumber").value(2));

        book(thirdToken, tripId)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No seats available on this trip"));
    }

    @Test
    void cancellingBookingReleasesTheSeat() throws Exception {
        String firstToken = createEmployeeWithUser();
        String secondToken = createEmployeeWithUser();
        String thirdToken = createEmployeeWithUser();

        String firstBooking = book(firstToken, tripId)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long bookingId = objectMapper.readTree(firstBooking).get("id").asLong();

        book(secondToken, tripId).andExpect(status().isCreated());

        mockMvc.perform(post("/api/bookings/{id}/cancel", bookingId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.seatNumber").doesNotExist());

        book(thirdToken, tripId)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seatNumber").value(1));
    }

    private org.springframework.test.web.servlet.ResultActions book(String token, Long tripId) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("tripId", tripId));
        return mockMvc.perform(post("/api/bookings")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String createEmployeeWithUser() throws Exception {
        String email = uniqueEmail("employee");
        Employee employee = new Employee();
        employee.setEmployeeCode(uniqueCode("EMP"));
        employee.setFullName("IT Employee");
        employee.setEmail(email);
        employee.setPhone("+919876543210");
        employee.setPickupAddress("IT Address");
        employee.setStatus(EmployeeStatus.ACTIVE);
        employeeRepository.save(employee);
        return registerAndGetToken(email, "EMPLOYEE");
    }
}
