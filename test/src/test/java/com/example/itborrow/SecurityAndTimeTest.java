package com.example.itborrow;

import java.time.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SecurityAndTimeTest {
    @Test void loginAccountLimitCannotBeBypassedByChangingSourceAndExpires() {
        var clock=org.mockito.Mockito.mock(Clock.class);
        Instant now=Instant.parse("2026-10-06T17:30:00Z");
        org.mockito.Mockito.when(clock.instant()).thenReturn(now);
        var limiter=new com.example.itborrow.security.AuthenticationThrottle(clock,2,2);
        assertThat(limiter.check("one","Alice",false)).isZero();
        assertThat(limiter.check("two","alice",false)).isZero();
        assertThat(limiter.check("three","ALICE",false)).isPositive();
        org.mockito.Mockito.when(clock.instant()).thenReturn(now.plusSeconds(601));
        assertThat(limiter.check("three","alice",false)).isZero();
    }
    @Test void organizationalCalendarDoesNotFollowHostUtcDate() {
        var clock=Clock.fixed(Instant.parse("2026-10-06T17:30:00Z"),ZoneId.of("Asia/Bangkok"));
        assertThat(LocalDate.now(clock)).isEqualTo(LocalDate.of(2026,10,7));
        var request=new com.example.itborrow.domain.entity.BorrowRequest();
        request.setDueDate(LocalDate.of(2026,10,6));
        request.setStatus(com.example.itborrow.domain.enums.BorrowStatus.BORROWED);
        new com.example.itborrow.domain.state.BorrowedState(clock).markOverdue(request);
        assertThat(request.getStatus()).isEqualTo(com.example.itborrow.domain.enums.BorrowStatus.OVERDUE);
    }
    @Test void oversizedApiUploadHas413AndWebAvatarKeepsItsRedirect() {
        var handler=new com.example.itborrow.exception.GlobalExceptionHandler();
        var request=new org.springframework.mock.web.MockHttpServletRequest("POST","/api/v1/equipment/1/image");
        var result=(org.springframework.http.ResponseEntity<?>)handler.uploadTooLarge(request);
        assertThat(result.getStatusCode().value()).isEqualTo(413);
        assertThat(handler.uploadTooLarge(new org.springframework.mock.web.MockHttpServletRequest("POST","/profile/avatar")))
                .isInstanceOf(org.springframework.web.servlet.ModelAndView.class);
    }

    @Test void deliverySchedulingRemainsEnabledWhenOverdueIsDisabled() {
        schedulingContext().withPropertyValues("app.overdue.enabled=false","app.jobs.enabled=true","app.jobs.interval-ms=86400000")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(com.example.itborrow.config.DeliveryScheduler.class)
                            .doesNotHaveBean(com.example.itborrow.config.OverdueScheduler.class)
                            .hasSingleBean(org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor.class);
                });
    }
    @Test void overdueSchedulingRemainsEnabledWhenDeliveryIsDisabled() {
        schedulingContext().withPropertyValues("app.overdue.enabled=true","app.jobs.enabled=false","borrow.overdue.initial-delay-ms=86400000")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(com.example.itborrow.config.OverdueScheduler.class)
                        .doesNotHaveBean(com.example.itborrow.config.DeliveryScheduler.class));
    }
    private org.springframework.boot.test.context.runner.ApplicationContextRunner schedulingContext() {
        return new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(com.example.itborrow.config.SchedulingConfig.class,com.example.itborrow.config.OverdueScheduler.class,com.example.itborrow.config.DeliveryScheduler.class)
                .withBean(com.example.itborrow.service.PersistentJobs.class,()->org.mockito.Mockito.mock(com.example.itborrow.service.PersistentJobs.class))
                .withBean(com.example.itborrow.service.BorrowRequestService.class,()->org.mockito.Mockito.mock(com.example.itborrow.service.BorrowRequestService.class));
    }
}
