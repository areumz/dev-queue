package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataJpaUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
