package com.demo.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public User getUser(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    @Transactional(readOnly = true)
    public List<User> getGoldUsers() {
        return userRepo.findByTier("GOLD");
    }

    @Transactional
    public User createUser(String name, String email, String tier) {
        log.info("Creating user: {}", email);
        return userRepo.save(new User(name, email, tier));
    }
}
