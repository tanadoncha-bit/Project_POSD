package com.example.itborrow.service;
import com.example.itborrow.domain.entity.*;
import com.example.itborrow.domain.enums.Role;
import com.example.itborrow.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import java.util.UUID;
@Service
public class GoogleAccountService {
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final ExternalIdentityRepository identities;
    private final PasswordEncoder passwords;
    public GoogleAccountService(UserRepository users,UserProfileRepository profiles,ExternalIdentityRepository identities,PasswordEncoder passwords) {this.users=users;this.profiles=profiles;this.identities=identities;this.passwords=passwords;}
    @Transactional(readOnly=true) public boolean linked(String username) {return users.findByUsername(username).map(user -> identities.googleLinked(user.getId())).orElse(false);}
    @Transactional public User signIn(String subject,String email,boolean verified,String fullName,String linkingUsername) {
        if(subject==null || subject.isBlank() || subject.length()>255 || email==null || email.length()>100 || !verified)
            throw new OAuth2AuthenticationException(new OAuth2Error("unverified_email"),"Google must supply a verified email address.");
        var linked=identities.userId("google",subject);
        if(linked.isPresent()) {
            var account=users.findById(linked.get()).orElseThrow();
            if(account.getEmail().equalsIgnoreCase(email)) identities.markEmailVerified(account.getId(),account.getEmail());
            return account;
        }
        var existing=users.findByEmailIgnoreCase(email);
        User account;
        if(existing.isPresent()) {
            account=existing.get();
            if(linkingUsername!=null && !linkingUsername.equals(account.getUsername()))
                throw new OAuth2AuthenticationException(new OAuth2Error("email_mismatch"),"Use the Google account matching your profile email.");
            identities.lockUser(account.getId());
            if(!identities.emailVerified(account.getId(),account.getEmail()))
                throw new OAuth2AuthenticationException(new OAuth2Error("account_exists"),"Verify your email from your existing account before signing in with Google.");
            // Recheck after the lock so concurrent first-time logins do not create duplicate links.
            var concurrentLink=identities.userId("google",subject);
            if(concurrentLink.isPresent()) {
                if(!concurrentLink.get().equals(account.getId())) throw new OAuth2AuthenticationException(new OAuth2Error("identity_conflict"),"Google account is already connected to another account.");
                return account;
            }
            if(identities.googleLinked(account.getId()))
                throw new OAuth2AuthenticationException(new OAuth2Error("identity_conflict"),"This account is already connected to another Google account.");
        } else {
            if(linkingUsername!=null) throw new OAuth2AuthenticationException(new OAuth2Error("email_mismatch"),"Use the Google account matching your profile email.");
            account=new User("google_"+UUID.randomUUID().toString().replace("-",""),email,Role.USER);
            account.setPassword(passwords.encode(UUID.randomUUID().toString()+UUID.randomUUID().toString().substring(0,16)));
            account.setLocalPasswordEnabled(false);
            users.saveAndFlush(account);
            var profile=new UserProfile(fullName==null || fullName.isBlank() ? account.getUsername() : fullName.substring(0,Math.min(150,fullName.length())),null,null);
            profile.setUser(account);profiles.save(profile);
        }
        identities.link("google",subject,account.getId());
        identities.markEmailVerified(account.getId(),account.getEmail());
        return account;
    }
}
