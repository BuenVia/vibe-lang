package com.vibelang.sentence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * On update, a null {@code source} or {@code wrongVersion} keeps the stored value (older clients don't send them);
 * send an empty {@code wrongVersion} to clear it.
 */
public record SentenceRequest(
        @NotBlank @Size(max = 1000)
        @Pattern(regexp = "(?s).*\\{\\{[^{}]*\\S[^{}]*}}.*", message = "must contain at least one gap, e.g. {{tenga}}")
        String text,
        @Size(max = 1000) String translation,
        @Size(max = 200) String hint,
        String explanation,
        SentenceSource source,
        @Size(max = 1000) String wrongVersion) {

    /** The fields existing clients send; source and wrongVersion are then left as they are. */
    public SentenceRequest(String text, String translation, String hint, String explanation) {
        this(text, translation, hint, explanation, null, null);
    }
}
