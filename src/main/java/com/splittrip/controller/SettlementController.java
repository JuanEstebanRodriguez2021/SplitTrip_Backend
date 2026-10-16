package com.splittrip.controller;

import com.splittrip.dto.BalanceResponse;
import com.splittrip.dto.SettlementResponse;
import com.splittrip.security.CurrentUser;
import com.splittrip.service.SettlementService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @GetMapping("/balances")
    public List<BalanceResponse> getBalances(@PathVariable Long tripId, @AuthenticationPrincipal Jwt jwt) {
        return settlementService.getBalances(tripId, CurrentUser.id(jwt));
    }

    @GetMapping("/settlements")
    public List<SettlementResponse> getSettlements(@PathVariable Long tripId, @AuthenticationPrincipal Jwt jwt) {
        return settlementService.getSettlements(tripId, CurrentUser.id(jwt));
    }
}