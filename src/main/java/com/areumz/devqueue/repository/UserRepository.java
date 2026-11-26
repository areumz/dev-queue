package com.areumz.devqueue.repository;

import com.areumz.devqueue.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    /** 인증된 이메일만 찾는다. 같은 이메일을 가진 미인증 계정은 여러 개일 수 있다. */
    List<User> findVerifiedByEmail(String email);
    List<User> findAll();
    void deleteById(Long id);
}
