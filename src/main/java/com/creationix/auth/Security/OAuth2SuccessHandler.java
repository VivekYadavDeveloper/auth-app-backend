package com.creationix.auth.Security;

import com.creationix.auth.Entities.Provider;
import com.creationix.auth.Entities.RefreshToken;
import com.creationix.auth.Entities.User;
import com.creationix.auth.Repositories.RefreshTokenRepository;
import com.creationix.auth.Repositories.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;


@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final CookiesService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.auth.frontend.success-redirect}")
    private String frontEndSuccessUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        logger.info("Successful authentication");
        logger.info(authentication.toString());

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        /*IDENTIFY USER EXIST*/
        /*Yaha par hum user ko database me save karenge agar wo pehle se exist nahi karta hai*/
        String registrationId = "unknown";
        if (authentication instanceof OAuth2AuthenticationToken token) {
            registrationId = token.getAuthorizedClientRegistrationId();
        }

        logger.info("Registration Id: {}", registrationId);
        logger.info("Username : {}", oAuth2User.getAttributes().toString());

        User user;

        switch (registrationId) {
            case "google" -> {
                String googleId = oAuth2User.getAttributes().getOrDefault("sub", "").toString();
                String email = oAuth2User.getAttributes().getOrDefault("email", "").toString();
                String name = oAuth2User.getAttributes().getOrDefault("name", "").toString();
                String picture = oAuth2User.getAttributes().getOrDefault("picture", "").toString();

                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(picture)
                        .enabled(true)
                        .provider(Provider.GOOGLE)
                        .providerId(googleId)
                        .build();

                user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));
            }
            case "github" -> {
                String name = oAuth2User.getAttributes().getOrDefault("login", "").toString();
                String githubId = oAuth2User.getAttributes().getOrDefault("id", "").toString();
                String image = oAuth2User.getAttributes().getOrDefault("avatar_url", "").toString();
                String email = (String) oAuth2User.getAttributes().get("email");

                if (email == null) {
                    email = name + "@github.com";
                }
                User newUser = User.builder()
                        .email(email)
                        .name(name)
                        .image(image)
                        .enabled(true)
                        .provider(Provider.GITHUB)
                        .providerId(githubId)
                        .build();
                user = userRepository.findByEmail(email).orElseGet(() -> userRepository.save(newUser));

            }

            default -> {
                logger.warn("Unsupported registration id: {}", registrationId);
                throw new RuntimeException("Unsupported authentication provider: " + registrationId); // ✅ throw so compiler knows user is always assigned
            }
        }



        /* GENERATE NEW REFRESH TOKEN WHAT IT'LL DO IT GIVES/GENERATE NEW ACCESS TOKEN*/
        /* ✅ user is accessible here now*/

        String jti = UUID.randomUUID().toString();
        RefreshToken refreshTokenOb = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .revoked(false)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSecond()))
                .build();

        refreshTokenRepository.save(refreshTokenOb);

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOb.getJti());
        cookieService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSecond());
        /*TO CHECK THE OAUTH2 IS WORKING OR NOT USE THIS URL FOR GOOGLE LOGIN "http://localhost:8083/oauth2/authorization/google"*/
        response.getWriter().write("Authentication successful. You can close this window and return to the application.");
        response.sendRedirect(frontEndSuccessUrl);
    }
}