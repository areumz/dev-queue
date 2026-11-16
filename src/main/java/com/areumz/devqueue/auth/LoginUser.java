package com.areumz.devqueue.auth;

import com.areumz.devqueue.domain.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** What the session holds for a logged-in user. The password is erased after login. */
public class LoginUser implements UserDetails, CredentialsContainer {
    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String username;
    private final String nickname;
    private String password;

    public LoginUser(Long id, String username, String nickname, String password) {
        this.id = id;
        this.username = username;
        this.nickname = nickname;
        this.password = password;
    }

    public static LoginUser from(User user) {
        return new LoginUser(user.getId(), user.getUsername(), user.getNickname(), user.getPassword());
    }

    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
