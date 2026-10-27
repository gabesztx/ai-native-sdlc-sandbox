package com.hugoboard.review;

import jakarta.validation.constraints.NotNull;

public record AnswerInput(@NotNull Long questionId, Integer scaleValue, String textValue) {
}
