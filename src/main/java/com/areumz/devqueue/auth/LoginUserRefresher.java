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

/** 마이페이지 정보 수정 후 로그인 사용자 정보 갱신 */
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
        // Security 6+ : 세션 변경 자체를 자동으로 저장하지 않음; 명시적으로 저장
        securityContextRepository.saveContext(context, request, response);
    }
}
