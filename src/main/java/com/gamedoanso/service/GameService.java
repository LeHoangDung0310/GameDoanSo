package com.gamedoanso.service;

import com.gamedoanso.dto.GuessRequest;
import com.gamedoanso.dto.GuessResponse;
import com.gamedoanso.entity.User;
import com.gamedoanso.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameService {

    private final UserRepository userRepository;
    private final Random random = new Random();

    @Transactional
    public GuessResponse processGuess(String username, GuessRequest request) {
        User user = userRepository.findWithLockByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTurns() <= 0) {
            return GuessResponse.builder()
                    .won(false)
                    .remainingTurns(0)
                    .currentScore(user.getScore())
                    .message("You have no turns left, You need to buy more turns")
                    .build();
        }

        user.setTurns(user.getTurns() - 1);
        int guess = request.getGuess();
        int serverNumber;
        boolean won;

        double winrate = 0.05;
        if (random.nextDouble() < winrate) {
            serverNumber = guess;
            won = true;
            user.setScore(user.getScore() + 1);
        } else {
            won = false;
            serverNumber = random.nextInt(5) + 1;
            while (serverNumber == guess) {
                serverNumber = random.nextInt(5) + 1;
            }
        }

        userRepository.save(user);

        return GuessResponse.builder()
                .serverNumber(serverNumber)
                .won(won)
                .remainingTurns(user.getTurns())
                .currentScore(user.getScore())
                .message(won ? "Congratulations! You made the right choice." : "You made the wrong choice.")
                .build();
    }

    @Transactional
    public com.gamedoanso.dto.UserProfileResponse buyTurns(String username) {
        User user = userRepository.findWithLockByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTurns(user.getTurns() + 5);
        userRepository.save(user);

        return com.gamedoanso.dto.UserProfileResponse.builder()
                .username(user.getUsername())
                .score(user.getScore())
                .turns(user.getTurns())
                .message("Successfully purchased 10 turns!")
                .build();
    }
}
