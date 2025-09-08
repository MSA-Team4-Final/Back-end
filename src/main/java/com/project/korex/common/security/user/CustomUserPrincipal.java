package com.project.korex.common.security.user;

import com.project.korex.user.entity.Users;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Getter
public class CustomUserPrincipal implements UserDetails{

    private final Users user;
    private final Map<String, Object> attributes;
    private final String attributeKey;
    private final String registrationId;

    public Long getUserId() {
        return user != null ? user.getId() : null;
    }

    //일반 Login 생성
    public CustomUserPrincipal(Users user) {
        this.user = user;
        this.attributes = null;
        this.attributeKey = null;
        this.registrationId = null;
    }

    //공통
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(user.getRole().getRoleName()));

        if (Boolean.TRUE.equals(user.isRestricted())) {
            authorities.add(new SimpleGrantedAuthority("RESTRICTED"));
        } else {
            authorities.add(new SimpleGrantedAuthority("UNLOCKED"));
        }

        return authorities;
    }

    //loginId 반환
    public String getName() {
            return user.getLoginId();
    }

    //name 반환
    @Override
    public String getUsername() {
            return user.getName();
    }

    public String getEmail() {
        return user.getEmail();
    }

    //UserDetails
    @Override
    public String getPassword() {
        return null;
    }

    public boolean isRestricted() {
        return Boolean.TRUE.equals(user.isRestricted());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
