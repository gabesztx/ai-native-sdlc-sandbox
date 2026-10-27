package com.hugoboard.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FormInput(
		@NotBlank @Size(max = 200) String topic,
		@NotNull Long evaluatedMemberId,
		@NotNull List<@NotNull @Valid QuestionInput> questions) {
}
