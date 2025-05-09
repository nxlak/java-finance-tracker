package backend.academy.scrapper.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record TransactionRequestDto(
        @NotNull @Positive double amount,
        @NotBlank String category,
        @NotBlank String description,
        @NotNull @PastOrPresent LocalDateTime timestamp
) {}