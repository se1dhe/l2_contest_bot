package dev.se1dhe.bot.payments;

import lombok.Data;

@Data
public class ProjectInfoResponse {
    private String status;
    private ProjectInfo result;
}