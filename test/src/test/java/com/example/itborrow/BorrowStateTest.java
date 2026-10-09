package com.example.itborrow;

import static org.assertj.core.api.Assertions.*;

import com.example.itborrow.domain.entity.BorrowRequest;
import com.example.itborrow.domain.enums.BorrowStatus;
import com.example.itborrow.domain.state.*;
import com.example.itborrow.exception.InvalidBorrowStateException;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

class BorrowStateTest {
    private final Clock clock =
            Clock.fixed(Instant.parse("2026-10-09T03:00:00Z"), ZoneId.of("Asia/Bangkok"));
    private final List<BorrowState> states =
            List.of(
                    new PendingState(),
                    new ApprovedState(),
                    new BorrowedState(clock),
                    new OverdueState(),
                    new ReturnedState(),
                    new CancelledState());
    private final BorrowStateResolver resolver = new BorrowStateResolver(states);
    private final Map<String, BiConsumer<BorrowState, BorrowRequest>> actions =
            Map.of(
                    "approve",
                    BorrowState::approve,
                    "pickup",
                    BorrowState::pickUp,
                    "return",
                    BorrowState::returnEquipment,
                    "cancel",
                    BorrowState::cancel,
                    "overdue",
                    BorrowState::markOverdue);

    @TestFactory
    Stream<DynamicTest> everyStatusEnforcesAllowedTransitions() {
        return Stream.of(BorrowStatus.values())
                .flatMap(
                        status ->
                                actions.entrySet().stream()
                                        .map(
                                                action ->
                                                        DynamicTest.dynamicTest(
                                                                status + ": " + action.getKey(),
                                                                () -> {
                                                                    var request =
                                                                            new BorrowRequest();
                                                                    request.setStatus(status);
                                                                    request.setDueDate(
                                                                            LocalDate.now(clock)
                                                                                    .minusDays(1));
                                                                    BorrowStatus expected =
                                                                            expected(
                                                                                    status,
                                                                                    action
                                                                                            .getKey());
                                                                    if (expected == null) {
                                                                        assertThatThrownBy(
                                                                                        () ->
                                                                                                action.getValue()
                                                                                                        .accept(
                                                                                                                resolver
                                                                                                                        .resolve(
                                                                                                                                status),
                                                                                                                request))
                                                                                .isInstanceOf(
                                                                                        InvalidBorrowStateException
                                                                                                .class);
                                                                        assertThat(
                                                                                        request
                                                                                                .getStatus())
                                                                                .isEqualTo(status);
                                                                    } else {
                                                                        action.getValue()
                                                                                .accept(
                                                                                        resolver
                                                                                                .resolve(
                                                                                                        status),
                                                                                        request);
                                                                        assertThat(
                                                                                        request
                                                                                                .getStatus())
                                                                                .isEqualTo(
                                                                                        expected);
                                                                    }
                                                                })));
    }

    private BorrowStatus expected(BorrowStatus status, String action) {
        if (status == BorrowStatus.PENDING && action.equals("approve"))
            return BorrowStatus.APPROVED;
        if ((status == BorrowStatus.PENDING || status == BorrowStatus.APPROVED)
                && action.equals("cancel")) return BorrowStatus.CANCELLED;
        if (status == BorrowStatus.APPROVED && action.equals("pickup"))
            return BorrowStatus.BORROWED;
        if ((status == BorrowStatus.BORROWED || status == BorrowStatus.OVERDUE)
                && action.equals("return")) return BorrowStatus.RETURNED;
        if (status == BorrowStatus.BORROWED && action.equals("overdue"))
            return BorrowStatus.OVERDUE;
        return null;
    }

    @Test
    void dueTodayDoesNotBecomeOverdue() {
        var request = new BorrowRequest();
        request.setStatus(BorrowStatus.BORROWED);
        request.setDueDate(LocalDate.now(clock));
        resolver.resolve(request.getStatus()).markOverdue(request);
        assertThat(request.getStatus()).isEqualTo(BorrowStatus.BORROWED);
    }

    @Test
    void cancelledRequestsHaveTheirOwnStateAndMessage() {
        var request = new BorrowRequest();
        request.setStatus(BorrowStatus.CANCELLED);
        assertThat(resolver.resolve(request.getStatus())).isInstanceOf(CancelledState.class);
        assertThatThrownBy(() -> resolver.resolve(request.getStatus()).returnEquipment(request))
                .isInstanceOf(InvalidBorrowStateException.class)
                .hasMessageContaining("CANCELLED")
                .hasMessageNotContaining("RETURNED");
    }

    @Test
    void duplicateStateRegistrationIsRejected() {
        var duplicates = new ArrayList<>(states);
        duplicates.add(new PendingState());
        assertThatThrownBy(() -> new BorrowStateResolver(duplicates))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void missingStateRegistrationIsRejected() {
        assertThatThrownBy(() -> new BorrowStateResolver(List.of(new PendingState())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing");
    }
}
