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

