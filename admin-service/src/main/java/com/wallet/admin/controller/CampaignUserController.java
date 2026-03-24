package com.wallet.admin.controller;

import com.wallet.admin.entity.Campaign;
import com.wallet.admin.service.AdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignUserController {
    private final AdminService adminService;

    public CampaignUserController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping
    public List<Campaign> getActiveCampaigns() {
        return adminService.getCampaigns().stream()
                .filter(c -> c.getEndDate() != null && !c.getEndDate().isBefore(java.time.LocalDate.now()))
                .collect(Collectors.toList());
    }
}
