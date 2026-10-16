package com.splittrip.controller;

import com.splittrip.dto.CreateExpenseRequest;
import com.splittrip.dto.ExpenseResponse;
import com.splittrip.security.CurrentUser;
import com.splittrip.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips/{tripId}/expenses")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @GetMapping
    public List<ExpenseResponse> listExpenses(@PathVariable Long tripId, @AuthenticationPrincipal Jwt jwt) {
        return expenseService.listExpenses(tripId, CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(@PathVariable Long tripId,
                                         @Valid @RequestBody CreateExpenseRequest request,
                                         @AuthenticationPrincipal Jwt jwt) {
        return expenseService.createExpense(tripId, request, CurrentUser.id(jwt));
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable Long tripId, @PathVariable Long expenseId,
                              @AuthenticationPrincipal Jwt jwt) {
        expenseService.deleteExpense(tripId, expenseId, CurrentUser.id(jwt));
    }
}