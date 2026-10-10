package com.example.itborrow;

import static org.assertj.core.api.Assertions.*;

import com.example.itborrow.config.DeliveryScheduler;
import com.example.itborrow.config.OverdueScheduler;
import com.example.itborrow.config.SchedulingConfig;
import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.domain.state.BorrowedState;
import com.example.itborrow.exception.GlobalExceptionHandler;
import com.example.itborrow.security.AuthenticationThrottle;
import com.example.itborrow.service.BorrowRequestService;
import com.example.itborrow.service.PersistentJobs;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.web.servlet.ModelAndView;

import java.time.*;

class SecurityAndTimeTest {
    @Test
    void loginAccountLimitCannotBeBypassedByChangingSourceAndExpires() {
        var clock = Mockito.mock(Clock.class);
        Instant now = Instant.parse("2026-10-06T17:30:00Z");
        Mockito.when(clock.instant()).thenReturn(now);
        var limiter = new AuthenticationThrottle(clock, 2, 2);
        assertThat(limiter.check("one", "Alice", false)).isZero();
        assertThat(limiter.check("two", "alice", false)).isZero();
        assertThat(limiter.check("three", "ALICE", false)).isPositive();
        Mockito.when(clock.instant()).thenReturn(now.plusSeconds(601));
        assertThat(limiter.check("three", "alice", false)).isZero();
    }

    @Test
    void organizationalCalendarDoesNotFollowHostUtcDate() {
        var clock = Clock.fixed(Instant.parse("2026-10-06T17:30:00Z"), ZoneId.of("Asia/Bangkok"));
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2026, 10, 7));
        var request = new BorrowRequest();
        request.setDueDate(LocalDate.of(2026, 10, 6));
        request.setStatus(BorrowStatus.BORROWED);
        new BorrowedState(clock).markOverdue(request);
        assertThat(request.getStatus()).isEqualTo(BorrowStatus.OVERDUE);
    }

    @Test
    void oversizedApiUploadHas413AndWebAvatarKeepsItsRedirect() {
        var handler = new GlobalExceptionHandler();
        var request = new MockHttpServletRequest("POST", "/api/v1/equipment/1/image");
        var result = (ResponseEntity<?>) handler.uploadTooLarge(request);
        assertThat(result.getStatusCode().value()).isEqualTo(413);
        assertThat(handler.uploadTooLarge(new MockHttpServletRequest("POST", "/profile/avatar")))
                .isInstanceOf(ModelAndView.class);
    }

    @Test
    void deliverySchedulingRemainsEnabledWhenOverdueIsDisabled() {
        schedulingContext()
                .withPropertyValues(
                        "app.overdue.enabled=false",
                        "app.jobs.enabled=true",
                        "app.jobs.interval-ms=86400000")
                .run(
                        context -> {
                            assertThat(context)
                                    .hasNotFailed()
                                    .hasSingleBean(DeliveryScheduler.class)
                                    .doesNotHaveBean(OverdueScheduler.class)
                                    .hasSingleBean(ScheduledAnnotationBeanPostProcessor.class);
                        });
    }

    @Test
    void overdueSchedulingRemainsEnabledWhenDeliveryIsDisabled() {
        schedulingContext()
                .withPropertyValues(
                        "app.overdue.enabled=true",
                        "app.jobs.enabled=false",
                        "borrow.overdue.initial-delay-ms=86400000")
                .run(
                        context ->
                                assertThat(context)
                                        .hasNotFailed()
                                        .hasSingleBean(OverdueScheduler.class)
                                        .doesNotHaveBean(DeliveryScheduler.class));
    }

    private ApplicationContextRunner schedulingContext() {
        return new ApplicationContextRunner()
                .withUserConfiguration(
                        SchedulingConfig.class, OverdueScheduler.class, DeliveryScheduler.class)
                .withBean(PersistentJobs.class, () -> Mockito.mock(PersistentJobs.class))
                .withBean(
                        BorrowRequestService.class, () -> Mockito.mock(BorrowRequestService.class));
    }
}
