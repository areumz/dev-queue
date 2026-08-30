package com.areumz.devqueue.repository.jpa;

import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.repository.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Profile("jpa")
public class JpaUserRepository implements UserRepository {
    private final SpringDataJpaUserRepository springDataJpaUserRepository;

    public JpaUserRepository(SpringDataJpaUserRepository springDataJpaUserRepository) {
        this.springDataJpaUserRepository = springDataJpaUserRepository;
    }

    @Override
    public User save(User user) {
        return springDataJpaUserRepository.save(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return springDataJpaUserRepository.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataJpaUserRepository.findByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return springDataJpaUserRepository.findAll();
    }
}
