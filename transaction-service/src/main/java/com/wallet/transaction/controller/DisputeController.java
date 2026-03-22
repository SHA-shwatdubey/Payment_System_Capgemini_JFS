package com.wallet.transaction.controller;

import com.wallet.transaction.dto.DisputeCreateRequest;
import com.wallet.transaction.dto.DisputeResolveRequest;
import com.wallet.transaction.entity.Dispute;
import com.wallet.transaction.service.DisputeService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping("/api/disputes")
    @ResponseStatus(HttpStatus.CREATED)
    public Dispute create(@RequestBody DisputeCreateRequest request) {
        return disputeService.create(request);
    }

    @GetMapping("/api/disputes")
    public List<Dispute> byUser(@RequestParam("userId") Long userId) {
        return disputeService.byUser(userId);
    }

    @GetMapping("/api/support/disputes/open")
    public List<Dispute> openDisputes() {
        return disputeService.openDisputes();
    }

    @PutMapping("/api/disputes/{id}/escalate")
    public Dispute escalate(@PathVariable("id") Long disputeId) {
        return disputeService.escalate(disputeId);
    }

    @PutMapping("/api/disputes/{id}/resolve")
    public Dispute resolve(@PathVariable("id") Long disputeId,
                           @RequestBody DisputeResolveRequest request) {
        return disputeService.resolve(disputeId, request);
    }
}


