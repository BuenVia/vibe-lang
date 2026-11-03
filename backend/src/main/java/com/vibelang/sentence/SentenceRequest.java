package com.vibelang.sentence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SentenceRequest(
        @NotBlank @Size(max = 1000)
        @Pattern(regexp = "(?s).*\\{\\{[^{}]*\\S[^{}]*}}.*", message = "must contain at least one gap, e.g. {{tenga}}")
        String text,
        @Size(max = 1000) String translation,
        @Size(max = 200) String hint,
        String explanation) {
}
