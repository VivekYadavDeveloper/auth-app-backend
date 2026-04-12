package com.creationix.auth.Controller;

import com.creationix.auth.Dto.LoginRequest;
import com.creationix.auth.Dto.RefreshTokenRequest;
import com.creationix.auth.Dto.TokenResponse;
import com.creationix.auth.Dto.UserDto;
import com.creationix.auth.Entities.RefreshToken;
import com.creationix.auth.Entities.User;
import com.creationix.auth.Repositories.RefreshTokenRepository;
import com.creationix.auth.Repositories.UserRepository;
import com.creationix.auth.Security.CookiesService;
import com.creationix.auth.Security.JwtService;
import com.creationix.auth.Services.AuthServices;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthServices authServices;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ModelMapper mapper;
    private final RefreshTokenRepository refreshTokenRepository;
    private final CookiesService cookiesService;

    /*LOGIN API*/
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest,
                                               HttpServletResponse response) {

        /*AUTHENTICATE*/
        Authentication authentication = authenticate(loginRequest);
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }

        /*GENERATE REFRESH TOKEN HERE AND SAVE THIER INFORMATION IN DB*/
        String jti = UUID.randomUUID().toString();
        var refreshTokenOB = RefreshToken.builder()
                .jti(jti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSecond()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshTokenOB);



        /*IF USER IS ENABLED HERE WE GENERATE ACCESS JWT TOKEN*/
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, refreshTokenOB.getJti());

        /*USE COOKIE SERVICE TO ATTACH REFRESH TOKEN IN COOKIE*/
        cookiesService.attachRefreshCookie(response, refreshToken, (int) jwtService.getRefreshTtlSecond());
        cookiesService.addNoStoreHeaders(response);

        TokenResponse tokenResponse = TokenResponse.of(accessToken, refreshToken, jwtService.getAccessTtlSecond(), mapper.map(user, UserDto.class));
        return ResponseEntity.ok(tokenResponse);
    }

    /*AUTHENTICATION VALIDATION FUNCTION*/
    private Authentication authenticate(LoginRequest loginRequest) {
        try {
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }


    /*ACCESS AND REFRESH TOKEN RENEW API*/
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody(required = false) RefreshTokenRequest body,
                                                      HttpServletRequest request,
                                                      HttpServletResponse response) {
        String refreshToken = readRefreshTokenFromRequest(body, request).orElseThrow(() -> new BadCredentialsException("Refresh token is missing"));
        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadCredentialsException("Invalid Refresh Token Type");
        }

        String jti = jwtService.getJti(refreshToken);
        UUID userId = jwtService.getUserId(refreshToken);
        RefreshToken storedRefreshToken = refreshTokenRepository.findByJti(jti).orElseThrow(() -> new BadCredentialsException("Refresh token not recognized"));

        if (storedRefreshToken.isRevoked()) {
            throw new BadCredentialsException("Refresh token expired or revoked");
        }

        if (storedRefreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Refresh token expired");
        }

        if (!storedRefreshToken.getUser().getId().equals(userId)) {
            throw new BadCredentialsException("Refresh token does not belong to this user");
        }
        /*REFRESH TOKEN KO ROTATE:*/
        /*TODO: NOTE Revoked the token in production level*/
        storedRefreshToken.setRevoked(true);
        String newJti = UUID.randomUUID().toString();
        storedRefreshToken.setReplacedByToken(newJti);
        refreshTokenRepository.save(storedRefreshToken);
        User user = storedRefreshToken.getUser();

        var newRefreshTokenOb = RefreshToken.builder()
                .jti(newJti)
                .user(user)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTtlSecond()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newRefreshTokenOb);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user, newRefreshTokenOb.getJti());

        cookiesService.attachRefreshCookie(response, newRefreshToken, (int) jwtService.getRefreshTtlSecond());
        cookiesService.addNoStoreHeaders(response);
        return ResponseEntity.ok(TokenResponse.of(newAccessToken, newRefreshToken, jwtService.getAccessTtlSecond(), mapper.map(user, UserDto.class)));


    }

    /*LOGOUT API*/
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        readRefreshTokenFromRequest(null, request).ifPresent(token -> {
            try {
                if (jwtService.isRefreshToken(token)) {
                    String jti = jwtService.getJti(token);
                    refreshTokenRepository.findByJti(jti).ifPresent(rt -> {
                        rt.setRevoked(true);
                        refreshTokenRepository.save(rt);
                    });
                }
            } catch (JwtException ignored) {
            }
        });

        /*Use CookieUtil (same behavior)*/
        cookiesService.clearRefreshCookie(response);
        cookiesService.addNoStoreHeaders(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /*THIS METHOD WILL HELP TO READ REFRESH TOKEN FROM REQUEST HEADER OR BODY */
    private Optional<String> readRefreshTokenFromRequest(RefreshTokenRequest body, HttpServletRequest request) {
        /*STEP 1. PREFER READ REFRESH TOKEN FROM COOKIES*/
        if (request.getCookies() != null) {
            Optional<String> fromCookie = Arrays
                    .stream(request.getCookies())
                    .filter(cookie -> cookiesService.getRefreshTokenCookieName().equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .filter(token -> !token.isBlank())
                    .findFirst();
            if (fromCookie.isPresent()) {
                return fromCookie;
            }

        }
        /*STEP 2. CHECK IF TOKEN IN BODY*/
        if (body != null && body.refreshToken() != null && !body.refreshToken().isBlank()) {
            return Optional.of(body.refreshToken());
        }
        /* STEP 3. CUSTOM HEADER*/
        String refreshHeader = request.getHeader("X-Refresh-Token");
        if (refreshHeader != null && !refreshHeader.isBlank()) {
            return Optional.of(refreshHeader);
        }

        /*STEP 3. Authorization = Bearer <token>*/
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String candidate = authHeader.substring(7).trim();
            if (!candidate.isEmpty()) {
                try {
                    if (jwtService.isRefreshToken(candidate)) {
                        return Optional.of(candidate);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return Optional.empty();
    }

    /*REGISTER USER API*/
    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto) {
        UserDto userCreated = authServices.registerUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userCreated);
    }
}
