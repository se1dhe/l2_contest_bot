package dev.se1dhe.bot.payments;

import lombok.Data;

import java.util.Map;

@Data
public class ProjectBalanceResponse {
    private String status;
    private Map<String, String> result;
}