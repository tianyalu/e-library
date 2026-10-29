package com.example.elibrary.api.controller;

import com.example.elibrary.application.service.ReconciliationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 运维端点，由 {@code SecurityConfig} 限制为 LIBRARIAN 角色。
 *
 * <p>对账报告拿快账计数器和实物副本行互相比。把它暴露出来，是想让偏差能被人看到，
 * 而不是只有跑批任务知道。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ops")
public class OpsController {

    private final ReconciliationService reconciliationService;

    public OpsController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Operation(summary = "库存与账目核对报告（只读）")
    @GetMapping("/reconciliation")
    public ReconciliationService.Report reconciliation() {
        return reconciliationService.check();
    }
}
