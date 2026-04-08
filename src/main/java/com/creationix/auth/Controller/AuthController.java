package com.creationix.auth.Controller;

import com.creationix.auth.Dto.LoginRequest;
import com.creationix.auth.Dto.TokenResponse;
import com.creationix.auth.Dto.UserDto;
import com.creationix.auth.Entities.User;
import com.creationix.auth.Repositories.UserRepository;
import com.creationix.auth.Security.JwtService;
import com.creationix.auth.Services.AuthServices;
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

@RestController
@RequestMapping("api/v1/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthServices authServices;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final ModelMapper mapper;

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest loginRequest) {

        /*AUTHENTICATE*/
        Authentication authentication = authenticate(loginRequest);
        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!user.isEnabled()) {
            throw new DisabledException("User is disabled");
        }
        /*IF USER IS ENABLED HERE WE GENERATE JWTTOKEN*/
        String accessToken = jwtService.generateAccessToken(user);
        TokenResponse tokenResponse = TokenResponse.of(accessToken, "", jwtService.getAccessTtlSecond(), mapper.map(user, UserDto.class));
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
