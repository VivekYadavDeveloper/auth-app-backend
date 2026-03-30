package com.creationix.auth.Services.Impls;

import com.creationix.auth.Dto.UserDto;
import com.creationix.auth.Services.AuthServices;
import com.creationix.auth.Services.UserServices;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthServices {

    private final UserServices userServices;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserDto registerUser(UserDto userDto) {
        /*Encode the simple password*/
        userDto.setPassword(passwordEncoder.encode(userDto.getPassword()));
        return userServices.createUser(userDto);
    }


}
