package com.areumz.devqueue.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.areumz.devqueue.domain.Role;
import com.areumz.devqueue.domain.User;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;

class LoginUserTest {

    private final LoginUser loginUser =
            LoginUser.from(new User("alice", "hashed", "앨리스", Role.BACKEND, null));

    @Test
    void survivesSerialization() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(loginUser);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            LoginUser copy = (LoginUser) in.readObject();
            assertThat(copy.getId()).isEqualTo(loginUser.getId());
            assertThat(copy.getUsername()).isEqualTo("alice");
            assertThat(copy.getNickname()).isEqualTo("앨리스");
        }
    }

    @Test
    void eraseCredentialsClearsPassword() {
        assertThat(loginUser.getPassword()).isEqualTo("hashed");
        loginUser.eraseCredentials();
        assertThat(loginUser.getPassword()).isNull();
    }
}
