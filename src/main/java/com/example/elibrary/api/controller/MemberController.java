package com.example.elibrary.api.controller;

import com.example.elibrary.api.dto.request.SettleFineRequest;
import com.example.elibrary.application.dto.FineView;
import com.example.elibrary.application.service.MemberAccountService;
import com.example.elibrary.application.usecase.SettleFineUseCase;
import com.example.elibrary.domain.model.FineStatus;
import com.example.elibrary.infrastructure.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会员自己的视图：现在借了什么（见 {@code /loans}）、欠了多少。
 *
 * <p>缴费的请求体可以不传，默认是 {@code PAID}：常规路径一个 URL 就够，
 * 将来管理员要记为 WAIVED 也留了口子。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/members/me")
public class MemberController {

    private final MemberAccountService memberAccountService;
    private final SettleFineUseCase settleFineUseCase;

    public MemberController(MemberAccountService memberAccountService, SettleFineUseCase settleFineUseCase) {
        this.memberAccountService = memberAccountService;
        this.settleFineUseCase = settleFineUseCase;
    }

    @Operation(summary = "当前借阅情况与剩余额度")
    @GetMapping("/summary")
    public MemberAccountService.AccountSummary summary(@AuthenticationPrincipal AuthenticatedMember currentMember) {
        return memberAccountService.summary(currentMember.memberId());
    }

    @Operation(summary = "未缴罚金")
    @GetMapping("/fines")
    public List<FineView> outstandingFines(@AuthenticationPrincipal AuthenticatedMember currentMember) {
        return memberAccountService.outstandingFines(currentMember.memberId());
    }

    @Operation(summary = "缴纳或减免一笔罚金")
    @PostMapping("/fines/{fineId}/settle")
    public SettleFineUseCase.Result settle(@AuthenticationPrincipal AuthenticatedMember currentMember,
                                           @PathVariable Long fineId,
                                           @RequestBody(required = false) @Valid SettleFineRequest request) {
        FineStatus target = request == null ? FineStatus.PAID : request.targetStatus();
        return settleFineUseCase.execute(currentMember.memberId(), fineId, target);
    }
}
