package com.areumz.devqueue.service;

import com.areumz.devqueue.domain.EmailTokenType;
import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.exception.DuplicateUsernameException;
import com.areumz.devqueue.exception.ResourceNotFoundException;
import com.areumz.devqueue.exception.WrongPasswordException;
import com.areumz.devqueue.repository.ArticleRepository;
import com.areumz.devqueue.repository.UserRepository;
import com.areumz.devqueue.repository.VocabularyRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final VocabularyRepository vocabularyRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UserService(UserRepository userRepository, ArticleRepository articleRepository,
                       VocabularyRepository vocabularyRepository, PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.userRepository = userRepository;
        this.articleRepository = articleRepository;
        this.vocabularyRepository = vocabularyRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
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

        // 2. 비밀번호 암호화 후 저장. 이메일은 중복을 검사하지 않는다(인증할 때 검사한다).
        String encodedPassword = passwordEncoder.encode(rawPassword);
        return userRepository.save(
                new User(username, encodedPassword, nickname, role, roleDetail, normalizeEmail(email)));
    }

    public User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    }

    /** 이메일이 바뀌었으면 true. 이메일 중복은 여기서 검사하지 않는다(인증할 때 검사한다). */
    @Transactional
    public boolean updateProfile(Long userId, String nickname, Role role, String roleDetail, String email) {
        User user = getUser(userId);
        boolean emailChanged = user.updateProfile(nickname, role, role == Role.ETC ? roleDetail : null,
                normalizeEmail(email));
        if (emailChanged) {
            // 이전 주소로 보낸 인증 링크는 이메일 변경과 같은 트랜잭션에서 죽여야 한다(사이에 끼어들어 새 주소를 인증하는 경쟁 방지)
            tokenService.invalidateOpen(userId, EmailTokenType.VERIFY_EMAIL);
        }
        return emailChanged;
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new WrongPasswordException();
        }
        user.changePassword(passwordEncoder.encode(newPassword));
    }

    @Transactional
    public void withdraw(Long userId, String rawPassword) {
        User user = getUser(userId);
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new WrongPasswordException();
        }
        vocabularyRepository.deleteByUserId(userId);   // 단어 -> 글 -> 토큰 -> 계정 순서
        articleRepository.deleteByUserId(userId);
        tokenService.deleteAllOf(userId);
        userRepository.deleteById(userId);
    }
}
