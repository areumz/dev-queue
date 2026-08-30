package com.areumz.devqueue.repository.memory;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryUserRepositoryTest {
    MemoryUserRepository repository = new MemoryUserRepository();

    @AfterEach
    public void afterEach() {
        repository.clearUsers();
    }

    @Test
   public void save() {
        User user = new User("user1", "a123", "user_1", Role.FRONTEND, null);
        repository.save(user);

        User result = repository.findById(user.getId()).get();
        assertThat(user).isEqualTo(result);
    }

    @Test
    public void findUsername() {
        User user1 = new User("user1", "a123", "user_1", Role.APP, null);
        repository.save(user1);

        User result = repository.findByUsername("user1").get();
        assertThat(result).isEqualTo(user1);
    }

    @Test
    public void findAll() {
        User user1 = new User("user1", "a123", "user_1", Role.FRONTEND, null);
        repository.save(user1);

        User user2 = new User("user2", "a123", "user_2", Role.FRONTEND, null);
        repository.save(user2);

        List<User> result = repository.findAll();

        assertThat(result.size()).isEqualTo(2);

    }
}
