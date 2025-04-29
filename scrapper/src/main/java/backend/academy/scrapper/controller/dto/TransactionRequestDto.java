package backend.academy.scrapper.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record TransactionRequestDto(
        @NotNull double amount,
        @NotBlank String category,
        @NotBlank String description,
        @NotNull LocalDateTime timestamp
) {}
