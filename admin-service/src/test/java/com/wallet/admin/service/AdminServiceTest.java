package com.wallet.admin.service;

import com.wallet.admin.client.UserKycClient;
import com.wallet.admin.dto.CampaignRequest;
import com.wallet.admin.dto.KycApprovalRequest;
import com.wallet.admin.entity.AdminAction;
import com.wallet.admin.entity.Campaign;
import com.wallet.admin.repository.AdminActionRepository;
import com.wallet.admin.repository.CampaignRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private AdminActionRepository adminActionRepository;

    @Mock
    private UserKycClient userKycClient;

    @InjectMocks
    private AdminService adminService;

    @Test
    void createCampaign_savesAndReturnsEntity() {
        CampaignRequest request = new CampaignRequest("Cashback", "TOPUP", 20, LocalDate.now(),
                LocalDate.now().plusDays(3));
        Campaign saved = new Campaign();
        saved.setId(1L);
        saved.setName("Cashback");

        when(campaignRepository.save(any(Campaign.class))).thenReturn(saved);

        Campaign result = adminService.createCampaign(request);

        assertThat(result.getId()).isEqualTo(1L);
        verify(campaignRepository).save(any(Campaign.class));
    }

    @Test
    void approveKyc_savesAdminActionAndReturnsResponse() {
        KycApprovalRequest request = new KycApprovalRequest("APPROVED", "ok");
        when(userKycClient.updateStatus(5L, request)).thenReturn(Map.of("status", "APPROVED"));

        Object response = adminService.approveKyc(5L, request);

        assertThat(response).isEqualTo(Map.of("status", "APPROVED"));
        ArgumentCaptor<AdminAction> captor = ArgumentCaptor.forClass(AdminAction.class);
        verify(adminActionRepository).save(captor.capture());
        assertThat(captor.getValue().getActionType()).isEqualTo("KYC_REVIEW");
        assertThat(captor.getValue().getTargetId()).isEqualTo(5L);
    }

    @Test
    void dashboard_aggregatesCounters() {
        when(campaignRepository.count()).thenReturn(3L);
        when(adminActionRepository.count()).thenReturn(8L);

        List<Object> pending = new ArrayList<>();
        pending.add(Map.of("id", 1));
        pending.add(Map.of("id", 2));
        when(userKycClient.pendingKyc()).thenReturn(pending);

        Map<String, Object> result = adminService.dashboard();

        assertThat(result.get("totalCampaigns")).isEqualTo(3L);
        assertThat(result.get("totalAdminActions")).isEqualTo(8L);
        assertThat(result.get("pendingKyc")).isEqualTo(2);
    }

    @Test
    void pendingKyc_delegatesToClient() {
        List<Object> pending = List.of(Map.of("userId", 88L));
        when(userKycClient.pendingKyc()).thenReturn(pending);

        List<Object> response = adminService.pendingKyc();

        assertThat(response).hasSize(1);
        assertThat(response.get(0)).isEqualTo(Map.of("userId", 88L));
        verify(userKycClient).pendingKyc();
    }

    @Test
    void approveKyc_whenClientFails_doesNotSaveAdminAction() {
        KycApprovalRequest request = new KycApprovalRequest("REJECTED", "bad doc");
        when(userKycClient.updateStatus(7L, request)).thenThrow(new RuntimeException("downstream error"));

        assertThatThrownBy(() -> adminService.approveKyc(7L, request)).isInstanceOf(RuntimeException.class);
        verify(adminActionRepository, never()).save(any(AdminAction.class));
    }

    @Test
    void updateCampaign_withVariousUpdates_savesCorrectly() {
        Campaign campaign = new Campaign();
        campaign.setId(10L);
        campaign.setName("Old Name");
        when(campaignRepository.findById(10L)).thenReturn(java.util.Optional.of(campaign));
        when(campaignRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Map<String, Object> updates = Map.of(
            "name", "New Name",
            "status", "INACTIVE",
            "bonusPoints", 50
        );

        Campaign result = adminService.updateCampaign(10L, updates);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getStatus()).isEqualTo("INACTIVE");
        assertThat(result.isActive()).isFalse();
        assertThat(result.getBonusPoints()).isEqualTo(50);
    }

    @Test
    void activateAndDeactivate_updatesStatusAndActiveFlag() {
        Campaign campaign = new Campaign();
        campaign.setId(1L);
        when(campaignRepository.findById(1L)).thenReturn(java.util.Optional.of(campaign));
        when(campaignRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        adminService.activateCampaign(1L);
        assertThat(campaign.isActive()).isTrue();
        assertThat(campaign.getStatus()).isEqualTo("ACTIVE");

        adminService.deactivateCampaign(1L);
        assertThat(campaign.isActive()).isFalse();
        assertThat(campaign.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    void deleteCampaign_callsRepository() {
        adminService.deleteCampaign(1L);
        verify(campaignRepository).deleteById(1L);
    }

    @Test
    void getCampaigns_returnsAll() {
        when(campaignRepository.findAll()).thenReturn(List.of(new Campaign()));
        List<Campaign> result = adminService.getCampaigns();
        assertThat(result).hasSize(1);
    }
}
