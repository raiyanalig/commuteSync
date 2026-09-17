package com.commutesync.employee.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commutesync.common.exception.ResourceNotFoundException;
import com.commutesync.employee.domain.EmployeeStatus;
import com.commutesync.employee.dto.EmployeeResponse;
import com.commutesync.employee.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EmployeeController.class)
@AutoConfigureMockMvc(addFilters = false)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @Test
    void createReturns201WithBody() throws Exception {
        when(employeeService.create(any())).thenReturn(response());

        String body = objectMapper.writeValueAsString(Map.of(
                "employeeCode", "EMP001",
                "fullName", "Raiyan Ali",
                "email", "raiyan@example.com",
                "phone", "+919876543210",
                "pickupAddress", "12 MG Road",
                "status", "ACTIVE"));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createRejectsInvalidPayloadWithFieldErrors() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "employeeCode", "",
                "fullName", "",
                "email", "not-an-email",
                "phone", "abc",
                "pickupAddress", ""));

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.employeeCode").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists());
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        when(employeeService.getById(99L)).thenThrow(new ResourceNotFoundException("Employee", 99L));

        mockMvc.perform(get("/api/employees/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id: 99"));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/employees/1"))
                .andExpect(status().isNoContent());

        verify(employeeService).delete(1L);
    }

    private EmployeeResponse response() {
        return new EmployeeResponse(
                1L, "EMP001", "Raiyan Ali", "raiyan@example.com", "+919876543210",
                "12 MG Road", "Stop A", 5L, EmployeeStatus.ACTIVE, null, null);
    }
}
