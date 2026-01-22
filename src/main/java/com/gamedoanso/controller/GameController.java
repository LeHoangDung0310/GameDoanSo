package com.gamedoanso.controller;

import com.gamedoanso.dto.GuessRequest;
import com.gamedoanso.dto.GuessResponse;
import com.gamedoanso.dto.UserProfileResponse;
import com.gamedoanso.service.GameService;
import lombok.RequiredArgsConstructor;

import java.util.List;

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
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody GuessRequest request) {
        return ResponseEntity.ok(gameService.processGuess(userDetails.getUsername(), request));
    }

    // @PostMapping("/buy-turns")
    // public ResponseEntity<UserProfileResponse> buyTurns(
    // @AuthenticationPrincipal UserDetails userDetails) {
    // return ResponseEntity.ok(gameService.buyTurns(userDetails.getUsername()));
    // }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<UserProfileResponse>> getLeaderboard() {
        return ResponseEntity.ok(gameService.getLeaderboard());
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(gameService.getUserProfile(userDetails.getUsername()));
    }
}
