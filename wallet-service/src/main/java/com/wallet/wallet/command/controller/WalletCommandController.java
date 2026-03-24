package com.wallet.wallet.command.controller;

import com.wallet.wallet.command.dto.WalletCommandStatusResponse;
import com.wallet.wallet.command.dto.WalletUpdateCommand;
import com.wallet.wallet.command.handler.WalletCommandHandler;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
public class WalletCommandController {

    private final WalletCommandHandler walletCommandHandler;

    public WalletCommandController(WalletCommandHandler walletCommandHandler) {
        this.walletCommandHandler = walletCommandHandler;
    }

    @PutMapping("/update")
    public ResponseEntity<WalletCommandStatusResponse> update(@Valid @RequestBody WalletUpdateCommand command) {
        walletCommandHandler.handle(command);
        return ResponseEntity.accepted().body(
                WalletCommandStatusResponse.accepted(command.commandId(), "Wallet command accepted")
        );
    }
}

