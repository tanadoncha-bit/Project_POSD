package com.example.itborrow;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.itborrow.config.FeePolicyProperties;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.domain.state.BorrowState;
import com.example.itborrow.domain.state.BorrowStateResolver;
import com.example.itborrow.exception.GlobalExceptionHandler;
import com.example.itborrow.service.strategy.FineStrategyResolver;
import com.example.itborrow.service.strategy.StandardFineStrategy;
import com.example.itborrow.service.strategy.VipFineStrategy;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.Map;
import java.util.Set;

class ErrorHandlingRegressionTest {
    @RestController
    static class Probe {
        @GetMapping("/api/probe/{id}")
        Map<String, Long> get(@PathVariable("id") Long id, @RequestParam("count") int count) {
            return Map.of("id", id);
        }

        @PostMapping(value = "/api/probe", consumes = "application/json")
        Map<String, String> post(@RequestBody Map<String, String> body) {
            return body;
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("do-not-expose-sensitive-message");
        }

        @GetMapping("/denied")
        void denied() {
            throw new AccessDeniedException("denied");
        }

        @GetMapping("/conflict")
        void conflict() {
            throw new DataIntegrityViolationException("internal constraint");
        }
    }

    private final MockMvc mvc =
            MockMvcBuilders.standaloneSetup(new Probe())
                    .setControllerAdvice(new GlobalExceptionHandler())
                    .build();

    @Test
    void badTypesRemain400() throws Exception {
        mvc.perform(get("/api/probe/abc").param("count", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingParametersRemain400() throws Exception {
        mvc.perform(get("/api/probe/1")).andExpect(status().isBadRequest());
    }

    @Test
    void unsupportedMethodPreservesAllowHeader() throws Exception {
        mvc.perform(delete("/api/probe/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void unsupportedMediaTypeRemains415() throws Exception {
        mvc.perform(post("/api/probe").contentType("text/plain").content("hi"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void invalidJsonRemains400() throws Exception {
        mvc.perform(post("/api/probe").contentType("application/json").content("{bad"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void apiMethodErrorKeeps405WhenHtmlIsRequested() throws Exception {
        mvc.perform(delete("/api/probe/1").accept("text/html"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", containsString("GET")));
    }

    @Test
    void htmlUnexpectedErrorContainsSafeReference() throws Exception {
        var req = new MockHttpServletRequest("GET", "/unexpected");
        req.addHeader("Accept", "text/html");
        var result =
                (ModelAndView)
                        new GlobalExceptionHandler()
                                .handleUnexpected(
                                        new IllegalStateException(
                                                "do-not-expose-sensitive-message"),
                                        req);
        assertThat(result.getViewName()).isEqualTo("error/500");
        assertThat(result.getModel().get("requestId")).isNotNull();
        assertThat(result.getModel().toString()).doesNotContain("do-not-expose-sensitive-message");
    }

    @Test
    void browserDenialUses403View() throws Exception {
        var req = new MockHttpServletRequest("GET", "/denied");
        req.addHeader("Accept", "text/html");
        assertThat(
                        ((ModelAndView)
                                        new GlobalExceptionHandler()
                                                .denied(new AccessDeniedException("denied"), req))
                                .getViewName())
                .isEqualTo("error/403");
    }

    @Test
    void browserConflictUsesGeneric409View() throws Exception {
        var req = new MockHttpServletRequest("GET", "/conflict");
        req.addHeader("Accept", "text/html");
        var result =
                (ModelAndView)
                        new GlobalExceptionHandler()
                                .conflict(
                                        new DataIntegrityViolationException("private details"),
                                        req);
        assertThat(result.getViewName()).isEqualTo("error/generic");
        assertThat(result.getModel().get("statusCode")).isEqualTo(409);
        assertThat(result.getModel().toString()).doesNotContain("private details");
    }

    @Test
    void fineResolverRejectsDuplicateRoles() {
        var strategy = new StandardFineStrategy(new FeePolicyProperties());
        assertThatThrownBy(() -> new FineStrategyResolver(List.of(strategy, strategy)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fineResolverRequiresDefaultPolicy() {
        assertThatThrownBy(
                        () ->
                                new FineStrategyResolver(
                                        List.of(new VipFineStrategy(new FeePolicyProperties()))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void fineResolverRejectsNullRole() {
        var resolver =
                new FineStrategyResolver(
                        List.of(new StandardFineStrategy(new FeePolicyProperties())));
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(resolver.resolve(Role.ADMIN)).isInstanceOf(StandardFineStrategy.class);
    }

    @Test
    void stateResolverRejectsNullWithAControlledError() {
        var state = Mockito.mock(BorrowState.class);
        Mockito.when(state.supports()).thenReturn(Set.of(BorrowStatus.values()));
        var resolver = new BorrowStateResolver(List.of(state));
        assertThatThrownBy(() -> resolver.resolve(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("required");
    }
}
