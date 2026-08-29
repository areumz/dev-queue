package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User signup(String username, String rawPassword, String nickname) {
        // 1.아이디 중복 체크
        userRepository.findByUsername(username).ifPresent(user -> {
            throw new IllegalStateException("이미 존재하는 아이디입니다.");
        });

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // 3. User 생성 및 저장
        User user = new User(username, encodedPassword, nickname);
        return userRepository.save(user);
    }

    public User login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다"));

        if(!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }
}
