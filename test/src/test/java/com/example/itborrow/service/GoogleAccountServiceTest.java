package com.example.itborrow.service;
import com.example.itborrow.repository.*;
import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import java.util.Optional;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class GoogleAccountServiceTest {
    private final UserRepository users=mock(UserRepository.class);
    private final UserProfileRepository profiles=mock(UserProfileRepository.class);
    private final ExternalIdentityRepository identities=mock(ExternalIdentityRepository.class);
    private final PasswordEncoder encoder=mock(PasswordEncoder.class);
    private final GoogleAccountService service=new GoogleAccountService(users,profiles,identities,encoder);
    @Test void unverifiedEmailCannotCreateOrLinkAccount() {
        assertThatThrownBy(()->service.signIn("sub","a@gmail.com",false,"Alice",null)).isInstanceOf(OAuth2AuthenticationException.class);
        verifyNoInteractions(users,profiles,identities);
    }
    @Test void existingEmailCannotBeAutomaticallyLinked() {
        when(users.findByEmailIgnoreCase("a@gmail.com")).thenReturn(Optional.of(new User(1L,"alice","a@gmail.com",Role.ADMIN)));
        assertThatThrownBy(()->service.signIn("sub","a@gmail.com",true,"Alice",null)).isInstanceOf(OAuth2AuthenticationException.class);
        verify(identities,never()).link(anyString(),anyString(),anyLong());
    }
    @Test void signedInOwnerCanLinkAndKeepsExistingRole() {
        var owner=new User(1L,"alice","a@gmail.com",Role.ADMIN);
        when(users.findByEmailIgnoreCase("a@gmail.com")).thenReturn(Optional.of(owner));
        assertThat(service.signIn("sub","a@gmail.com",true,"Alice","alice")).isSameAs(owner);
        assertThat(owner.getRole()).isEqualTo(Role.ADMIN);
        verify(identities).link("google","sub",1L);verify(users,never()).saveAndFlush(any());
    }
    @Test void knownSubjectStillIdentifiesAccountAfterGoogleEmailChanges() {
        var owner=new User(1L,"alice","old@gmail.com",Role.VIP);
        when(identities.userId("google","stable-sub")).thenReturn(Optional.of(1L));when(users.findById(1L)).thenReturn(Optional.of(owner));
        assertThat(service.signIn("stable-sub","new@gmail.com",true,"Alice",null)).isSameAs(owner);
        verify(users,never()).findByEmailIgnoreCase(anyString());verify(identities,never()).markEmailVerified(anyLong(),anyString());
    }
    @Test void newGoogleAccountCreatesProfileAndOnlyUserRole() {
        when(encoder.encode(anyString())).thenReturn("hashed-unusable-local-password");
        when(users.saveAndFlush(any())).thenAnswer(call->{User user=call.getArgument(0);user.setId(7L);return user;});
        var account=service.signIn("new-sub","new@gmail.com",true,"New User",null);
        assertThat(account.getRole()).isEqualTo(Role.USER);assertThat(account.getPassword()).isEqualTo("hashed-unusable-local-password");
        verify(profiles).save(any(UserProfile.class));verify(identities).link("google","new-sub",7L);verify(identities).markEmailVerified(7L,"new@gmail.com");
    }
}
