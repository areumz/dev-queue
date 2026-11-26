package com.areumz.devqueue.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import com.areumz.devqueue.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@IntegrationTest
@Transactional
class UserRepositoryEmailTest {

    @Autowired
    UserRepository userRepository;

    private User save(String username, String email, boolean verified) {
        User user = new User(username, "x", username, Role.BACKEND, null, email);
        if (verified) {
            user.verifyEmail();
        }
        return userRepository.save(user);
    }

    @Test
    void onlyVerifiedOwnersAreFound() {
        User verified = save("alice", "a@x.com", true);
        save("bob", "a@x.com", false);
        save("carol", "c@x.com", true);

        assertThat(userRepository.findVerifiedByEmail("a@x.com")).containsExactly(verified);
    }

    @Test
    void severalUnverifiedAccountsMaySharePlainEmailWithoutErrors() {
        save("alice", "a@x.com", false);
        save("bob", "a@x.com", false);

        assertThat(userRepository.findVerifiedByEmail("a@x.com")).isEmpty();
    }
}
