package com.wallet.admin.controller;

import com.wallet.admin.dto.CampaignRequest;
import com.wallet.admin.dto.KycApprovalRequest;
import com.wallet.admin.entity.Campaign;
import com.wallet.admin.service.AdminService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @GetMapping("/campaigns")
    public List<Campaign> getCampaigns() {
        return adminService.getCampaigns();
    }

    @PatchMapping("/campaigns/{id}")
    public Campaign updateCampaign(@PathVariable("id") Long id, @RequestBody Map<String, Object> updates) {
        return adminService.updateCampaign(id, updates);
    }

    @DeleteMapping("/campaigns/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCampaign(@PathVariable("id") Long id) {
        adminService.deleteCampaign(id);
    }

    @PostMapping("/campaigns/{id}/deactivate")
    public Campaign deactivateCampaign(@PathVariable("id") Long id) {
        return adminService.deactivateCampaign(id);
    }

    @PostMapping("/campaigns/{id}/activate")
    public Campaign activateCampaign(@PathVariable("id") Long id) {
        return adminService.activateCampaign(id);
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        return adminService.dashboard();
    }
}
