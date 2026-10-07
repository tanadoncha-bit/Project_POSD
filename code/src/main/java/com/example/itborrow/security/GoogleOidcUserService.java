package com.example.itborrow.security;
import com.example.itborrow.service.GoogleAccountService;
import org.springframework.stereotype.Service;
import org.springframework.security.oauth2.client.oidc.userinfo.*;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.*;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import java.util.*;
@Service
public class GoogleOidcUserService implements OAuth2UserService<OidcUserRequest,OidcUser> {
    private final GoogleAccountService accounts;
    private final OidcUserService delegate=new OidcUserService();
    public GoogleOidcUserService(GoogleAccountService accounts) {this.accounts=accounts;}
    public OidcUser loadUser(OidcUserRequest request) {
        var google=delegate.loadUser(request); // Spring verifies issuer, signature, audience and nonce.
        var auth=SecurityContextHolder.getContext().getAuthentication();
        String linkingUsername=auth!=null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken) ? auth.getName() : null;
        var account=accounts.signIn(google.getSubject(),google.getEmail(),Boolean.TRUE.equals(google.getEmailVerified()),google.getFullName(),linkingUsername);
        Map<String,Object> claims=new HashMap<>(google.getClaims());claims.put("local_username",account.getUsername());claims.put("local_security_version",account.getSecurityVersion());
        return new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_"+account.getRole().name())),google.getIdToken(),new OidcUserInfo(claims),"local_username");
    }
}
