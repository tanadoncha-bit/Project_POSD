package com.example.itborrow.domain.state;

import com.example.itborrow.domain.enums.BorrowStatus;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class BorrowStateResolver {
    private final Map<BorrowStatus, BorrowState> states;
    public BorrowStateResolver(List<BorrowState> implementations) {
        var registered = new EnumMap<BorrowStatus, BorrowState>(BorrowStatus.class);
        for (var state : implementations) {
            for (var status : state.supports()) {
                if (registered.putIfAbsent(status, state) != null)
                    throw new IllegalStateException("Duplicate borrow state: " + status);
            }
        }
        for (var status : BorrowStatus.values()) {
            if (!registered.containsKey(status)) throw new IllegalStateException("Missing borrow state: " + status);
        }
        states = Map.copyOf(registered);
    }
    public BorrowState resolve(BorrowStatus status) {
        var state = states.get(status);
        if (state == null) throw new IllegalArgumentException("Unknown borrow state.");
        return state;
    }
}
