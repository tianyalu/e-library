package com.example.elibrary.application.usecase;

import com.example.elibrary.application.port.out.FineRepositoryPort;
import com.example.elibrary.domain.exception.ResourceNotFoundException;
import com.example.elibrary.domain.model.Fine;
import com.example.elibrary.domain.model.FineStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

/**
 * 结清一笔逾期罚金，让会员恢复借阅资格。
 *
 * <p>这里故意做得很薄：真实部署会把这一步换成对账务/支付网关的付款指令，
 * 但那条领域规则是一样的——欠着罚款就不能借。
 */
@Slf4j
@Service
public class SettleFineUseCase {

    public record Result(long fineId, long loanId, String amount, String status, Instant settledAt) {
    }

    private final FineRepositoryPort fines;
    private final Clock clock;

    public SettleFineUseCase(FineRepositoryPort fines, Clock clock) {
        this.fines = fines;
        this.clock = clock;
    }

    @Transactional
    public Result execute(Long memberId, Long fineId, FineStatus target) {
        Fine fine = fines.findById(fineId)
                .filter(f -> f.getMemberId() != null && f.getMemberId().equals(memberId))
                .orElseThrow(() -> new ResourceNotFoundException("Fine", fineId));
        fine.settle(target);
        Fine saved = fines.save(fine);
        Instant settledAt = clock.instant();
        log.info("Fine {} settled as {} for memberId={}", fineId, target, memberId);
        return new Result(saved.getId(), saved.getLoanId(), saved.getAmount().toString(), target.name(), settledAt);
    }
}
