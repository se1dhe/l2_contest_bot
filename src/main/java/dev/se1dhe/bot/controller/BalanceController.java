package dev.se1dhe.bot.controller;

import dev.se1dhe.bot.model.dto.AdjustBalanceRequest; // Import DTO
import dev.se1dhe.bot.model.dto.AdjustBalanceResponse; // Import DTO
import dev.se1dhe.bot.service.BalanceService;
import dev.se1dhe.bot.service.DBUserService;
import dev.se1dhe.bot.model.DbUser;
import dev.se1dhe.bot.exception.UserNotFoundException;
import dev.se1dhe.bot.exception.InsufficientFundsException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@RestController
@RequestMapping("/api/v1/balance")
public class BalanceController {

    @Value("${l2server.api.key}")
    private String apiKey;

    private final DBUserService dbUserService;
    private final BalanceService balanceService;

    @Autowired
    public BalanceController(DBUserService dbUserService, BalanceService balanceService) {
        this.dbUserService = dbUserService;
        this.balanceService = balanceService;
    }
    private boolean isValidApiKey(String providedApiKey) {
        if (providedApiKey == null || !providedApiKey.equals(apiKey))
            return false;
        else return true;
    }

    @PostMapping("/adjust")
    public ResponseEntity<?> adjustBalance(@RequestHeader("X-API-Key") String apiKey, @Valid @RequestBody AdjustBalanceRequest request) {
        if (!isValidApiKey(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid API Key");
        }

        DbUser user = null;
        try {
            user = dbUserService.findUserById(request.getTelegramId());
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }


        try {
            if(request.getAmount().compareTo(BigDecimal.ZERO) > 0){
                balanceService.deposit(request.getTelegramId(), request.getAmount());
            } else {
                balanceService.withdraw(request.getTelegramId(), request.getAmount().abs());
            }

        } catch (InsufficientFundsException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body("Insufficient funds");
        }
        catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error");
        }

        return ResponseEntity.ok(new AdjustBalanceResponse("success", "Balance adjusted successfully"));
    }
}