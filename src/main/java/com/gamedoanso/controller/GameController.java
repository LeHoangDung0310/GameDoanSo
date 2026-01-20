package com.gamedoanso.controller;

import com.gamedoanso.dto.GuessRequest;
import com.gamedoanso.dto.GuessResponse;
import com.gamedoanso.service.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/game")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/guess")
    public ResponseEntity<GuessResponse> guess(
            @RequestBody GuessRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        GuessResponse response = gameService.processGuess(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }
}
