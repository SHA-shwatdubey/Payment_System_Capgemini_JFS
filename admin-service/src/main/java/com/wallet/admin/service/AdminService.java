package com.wallet.admin.service;

import com.wallet.admin.client.UserKycClient;
import com.wallet.admin.dto.CampaignRequest;
import com.wallet.admin.dto.KycApprovalRequest;
import com.wallet.admin.entity.AdminAction;
import com.wallet.admin.entity.Campaign;
import com.wallet.admin.repository.AdminActionRepository;
import com.wallet.admin.repository.CampaignRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    private final CampaignRepository campaignRepository;
    private final AdminActionRepository adminActionRepository;
    private final UserKycClient userKycClient;

    public AdminService(CampaignRepository campaignRepository,
                        AdminActionRepository adminActionRepository,
                        UserKycClient userKycClient) {
        this.campaignRepository = campaignRepository;
        this.adminActionRepository = adminActionRepository;
        this.userKycClient = userKycClient;
    }

    public Campaign createCampaign(CampaignRequest request) {
        Campaign campaign = new Campaign();
        campaign.setName(request.name());
        campaign.setRuleType(request.ruleType());
        campaign.setBonusPoints(request.bonusPoints());
        campaign.setStartDate(request.startDate());
        campaign.setEndDate(request.endDate());
        campaign.setActive(true);
        campaign.setStatus("ACTIVE");
        return campaignRepository.save(campaign);
    }

    public List<Campaign> getCampaigns() {
        return campaignRepository.findAll();
    }

    public Campaign updateCampaign(Long id, Map<String, Object> updates) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
        if (updates.containsKey("status")) {
            String status = (String) updates.get("status");
            campaign.setStatus(status);
            campaign.setActive("ACTIVE".equalsIgnoreCase(status));
        }
        if (updates.containsKey("name")) {
            campaign.setName((String) updates.get("name"));
        }
        if (updates.containsKey("bonusPoints")) {
            campaign.setBonusPoints(((Number) updates.get("bonusPoints")).intValue());
        }
        return campaignRepository.save(campaign);
    }

    public void deleteCampaign(Long id) {
        campaignRepository.deleteById(id);
    }

    public Campaign deactivateCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
        campaign.setActive(false);
        campaign.setStatus("INACTIVE");
        return campaignRepository.save(campaign);
    }

    public Campaign activateCampaign(Long id) {
        Campaign campaign = campaignRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Campaign not found: " + id));
        campaign.setActive(true);
        campaign.setStatus("ACTIVE");
        return campaignRepository.save(campaign);
    }

    public List<Object> pendingKyc() {
        return userKycClient.pendingKyc();
    }

    public Object approveKyc(Long userId, KycApprovalRequest request) {
        Object response = userKycClient.updateStatus(userId, request);
        AdminAction action = new AdminAction();
        action.setActionType("KYC_REVIEW");
        action.setTargetId(userId);
        action.setStatus(request.status());
        action.setReason(request.reason());
        action.setCreatedAt(LocalDateTime.now());
        adminActionRepository.save(action);
        return response;
    }

    public Map<String, Object> dashboard() {
        return Map.of(
                "totalCampaigns", campaignRepository.count(),
                "totalAdminActions", adminActionRepository.count(),
                "pendingKyc", userKycClient.pendingKyc().size()
        );
    }
}
