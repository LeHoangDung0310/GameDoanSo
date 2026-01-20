package com.gamedoanso.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuessResponse {
    private int serverNumber;
    private boolean won;
    private int remainingTurns;
    private int currentScore;
    private String message;
}
