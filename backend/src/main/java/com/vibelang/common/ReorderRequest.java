package com.vibelang.common;

import jakarta.validation.constraints.NotNull;

import java.util.List;

/** The complete list of ids, in the desired order. */
public record ReorderRequest(@NotNull List<Long> ids) {
}
