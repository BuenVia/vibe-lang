package com.vibelang.common;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.stream.Collectors;

public final class Positions {

    private Positions() {
    }

    /**
     * Assigns positions 1..n to {@code items} following the order of {@code orderedIds}.
     * The ids must match the items exactly (no missing, extra or duplicate ids).
     */
    public static <T> void apply(List<T> items, List<Long> orderedIds, Function<T, Long> idOf, ObjIntConsumer<T> setPosition) {
        Map<Long, T> byId = items.stream().collect(Collectors.toMap(idOf, Function.identity()));
        if (orderedIds.size() != items.size() || !byId.keySet().equals(new HashSet<>(orderedIds))) {
            throw new IllegalArgumentException("The order must contain every item exactly once");
        }
        for (int i = 0; i < orderedIds.size(); i++) {
            setPosition.accept(byId.get(orderedIds.get(i)), i + 1);
        }
    }
}
