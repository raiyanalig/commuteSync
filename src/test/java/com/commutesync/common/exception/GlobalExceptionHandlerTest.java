package com.commutesync.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void resourceNotFoundMapsTo404() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Employee not found with id: 1"));
    }

    @Test
    void duplicateMapsTo409() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("duplicate"));
    }

    @Test
    void resourceInUseMapsTo409() throws Exception {
        mockMvc.perform(get("/test/in-use"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("in use"));
    }

    @Test
    void businessExceptionMapsTo400() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bad"));
    }

    @Test
    void accessDeniedMapsTo403() throws Exception {
        mockMvc.perform(get("/test/denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void validationMapsTo400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").value("name is required"));
    }

    @Test
    void malformedBodyMapsTo400() throws Exception {
        mockMvc.perform(post("/test/malformed")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void typeMismatchMapsTo400() throws Exception {
        mockMvc.perform(get("/test/type").param("count", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value 'abc' for parameter 'count'"));
    }

    @Test
    void unexpectedExceptionMapsTo500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error occurred"));
    }

    record SampleRequest(@NotBlank(message = "name is required") String name) {
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new ResourceNotFoundException("Employee", 1L);
        }

        @GetMapping("/test/duplicate")
        public void duplicate() {
            throw new DuplicateResourceException("duplicate");
        }

        @GetMapping("/test/in-use")
        public void inUse() {
            throw new ResourceInUseException("in use");
        }

        @GetMapping("/test/business")
        public void business() {
            throw new BusinessException("bad");
        }

        @GetMapping("/test/denied")
        public void denied() {
            throw new AccessDeniedException("denied");
        }

        @GetMapping("/test/boom")
        public void boom() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/test/type")
        public String type(@RequestParam int count) {
            return Integer.toString(count);
        }

        @PostMapping("/test/validate")
        public void validate(@Valid @RequestBody SampleRequest request) {
        }

        @PostMapping("/test/malformed")
        public void malformed(@RequestBody SampleRequest request) {
        }
    }
}
