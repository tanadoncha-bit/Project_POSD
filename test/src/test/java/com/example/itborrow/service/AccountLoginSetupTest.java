package com.example.itborrow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.itborrow.domain.entity.User;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.dto.request.LoginSetupDto;
import com.example.itborrow.repository.*;
import com.example.itborrow.security.PasswordPolicy;
import com.example.itborrow.service.impl.AccountServiceImpl;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

class AccountLoginSetupTest {
    private final UserRepository users = mock(UserRepository.class);
    private final CurrentUser current = mock(CurrentUser.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final AccountService service =
            new AccountServiceImpl(
                    users,
                    mock(UserProfileRepository.class),
                    encoder,
                    current,
                    new PasswordPolicy());

    private User account(boolean enabled) {
        var user = new User(7L, "google_original", "owner@gmail.com", Role.USER);
        user.setLocalPasswordEnabled(enabled);
        when(current.require()).thenReturn(user);
        when(users.findLockedById(7L)).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void googleAccountCanChooseCredentialsWithoutChangingItsIdentity() {
        var user = account(false);
        service.setupLogin(new LoginSetupDto("myusername", "chosen-password", "chosen-password"));
        assertThat(user.getId()).isEqualTo(7L);
        assertThat(user.getUsername()).isEqualTo("myusername");
        assertThat(user.isLocalPasswordEnabled()).isTrue();
        assertThat(encoder.matches("chosen-password", user.getPassword())).isTrue();
        verify(users).saveAndFlush(user);
    }

    @Test
    void existingLocalCredentialsCannotBeReplacedThroughSetup() {
        var user = account(true);
        assertThatThrownBy(
                        () ->
                                service.setupLogin(
                                        new LoginSetupDto("newname", "password1", "password1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getUsername()).isEqualTo("google_original");
        verify(users, never()).saveAndFlush(any());
    }

    @Test
    void duplicateUsernameDoesNotEnableLocalLogin() {
        var user = account(false);
        when(users.findByUsername("taken"))
                .thenReturn(Optional.of(new User(8L, "taken", "other@gmail.com", Role.USER)));
        assertThatThrownBy(
                        () ->
                                service.setupLogin(
                                        new LoginSetupDto("taken", "password1", "password1")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(user.isLocalPasswordEnabled()).isFalse();
        assertThat(user.getUsername()).isEqualTo("google_original");
        verify(users, never()).saveAndFlush(any());
    }
}
