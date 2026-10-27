package com.hugoboard.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SubmitInput(@NotNull @Size(max = 50) List<@NotNull @Valid AnswerInput> answers) {
}
