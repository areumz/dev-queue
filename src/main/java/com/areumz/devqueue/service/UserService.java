package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.exception.DuplicateEmailException;
import com.areumz.devqueue.exception.DuplicateUsernameException;
import com.areumz.devqueue.exception.ResourceNotFoundException;
import com.areumz.devqueue.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** The one place email is cleaned: trim, lowercase, blank becomes null. */
    public static String normalizeEmail(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    public User signup(String username, String rawPassword, String nickname,
                       Role role, String roleDetail, String email) {
        // 1. 아이디 중복 체크
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DuplicateUsernameException();
        }

        // 2. 이메일 정리 + 중복 체크 (이메일이 없으면 검사하지 않는다)
        String normalizedEmail = normalizeEmail(email);
        checkEmailAvailable(normalizedEmail, null);

        // 3. 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return userRepository.save(
                new User(username, encodedPassword, nickname, role, roleDetail, normalizedEmail));
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    }

    @Transactional
    public User updateProfile(Long userId, String nickname, Role role, String roleDetail, String email) {
        User user = getUser(userId);
        String normalizedEmail = normalizeEmail(email);
        checkEmailAvailable(normalizedEmail, userId);
        user.updateProfile(nickname, role, roleDetail, normalizedEmail);
        return user;
    }

    /** ownUserId: 수정 중인 본인 id. 가입이면 null. 본인의 기존 이메일은 중복으로 보지 않는다. */
    private void checkEmailAvailable(String email, Long ownUserId) {
        if (email == null) {
            return;
        }
        userRepository.findByEmail(email)
                .filter(found -> !found.getId().equals(ownUserId))
                .ifPresent(found -> {
                    throw new DuplicateEmailException();
                });
    }
}
