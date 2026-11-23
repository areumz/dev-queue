package com.areumz.devqueue.auth;

import com.areumz.devqueue.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/** Replaces the logged-in user in the session after the profile changed. */
@Component
public class LoginUserRefresher {

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    public void refresh(User user, HttpServletRequest request, HttpServletResponse response) {
        LoginUser principal = new LoginUser(user.getId(), user.getUsername(), user.getNickname(), null);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // Security 6+ does not persist context changes by itself; without this the session keeps the old value.
        securityContextRepository.saveContext(context, request, response);
    }
}
