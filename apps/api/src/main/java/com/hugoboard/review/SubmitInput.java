package com.hugoboard.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record SubmitInput(@NotNull List<@NotNull @Valid AnswerInput> answers) {
}
