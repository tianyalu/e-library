package com.example.elibrary.api.dto.request;

import com.example.elibrary.domain.model.FineStatus;
import jakarta.validation.constraints.NotNull;

public record SettleFineRequest(@NotNull FineStatus targetStatus) {
}
