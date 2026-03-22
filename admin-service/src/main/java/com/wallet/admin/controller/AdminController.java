package com.wallet.admin.controller;

import com.wallet.admin.dto.CampaignRequest;
import com.wallet.admin.dto.KycApprovalRequest;
import com.wallet.admin.entity.Campaign;
import com.wallet.admin.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/kyc/pending")
    public List<Object> pendingKyc() {
        return adminService.pendingKyc();
    }

    @PostMapping("/kyc/{userId}/approve")
    public Object approveKyc(@PathVariable("userId") Long userId, @RequestBody KycApprovalRequest request) {
        return adminService.approveKyc(userId, request);
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public Campaign createCampaign(@RequestBody CampaignRequest request) {
        return adminService.createCampaign(request);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return adminService.dashboard();
    }
}

