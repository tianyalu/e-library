package com.example.elibrary.application.dto;

import com.example.elibrary.domain.model.Fine;
import com.example.elibrary.domain.model.FineStatus;

import java.math.BigDecimal;

/** Read model of a penalty. */
public record FineView(Long fineId,
                       Long loanId,
                       BigDecimal amount,
                       String currency,
                       int daysOverdue,
                       FineStatus status) {

    public static FineView from(Fine fine) {
        return new FineView(fine.getId(), fine.getLoanId(), fine.getAmount().amount(),
                fine.getAmount().currency(), fine.getDaysOverdue(), fine.getStatus());
    }
}
