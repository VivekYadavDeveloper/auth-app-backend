package com.creationix.auth.Controller;

import com.creationix.auth.Dto.LoginRequest;
import com.creationix.auth.Dto.TokenResponse;
import com.creationix.auth.Dto.UserDto;
import com.creationix.auth.Entities.RefreshToken;
import com.creationix.auth.Entities.User;
import com.creationix.auth.Repositories.RefreshTokenRepository;
import com.creationix.auth.Repositories.UserRepository;
import com.creationix.auth.Security.CookiesService;
import com.creationix.auth.Security.JwtService;
import com.creationix.auth.Services.AuthServices;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
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


    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {

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

    private Authentication authenticate(LoginRequest loginRequest) {
        try {
            return authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid email or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> registerUser(@RequestBody UserDto userDto) {
        UserDto userCreated = authServices.registerUser(userDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userCreated);
    }
}
