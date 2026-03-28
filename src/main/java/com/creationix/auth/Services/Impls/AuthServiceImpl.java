package com.creationix.auth.Services.Impls;

import com.creationix.auth.Dto.UserDto;
import com.creationix.auth.Services.AuthServices;
import com.creationix.auth.Services.UserServices;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImpl implements AuthServices {

    private final UserServices userServices;
    @Override
    public UserDto registerUser(UserDto userDto) {
      UserDto userDto1=  userServices.createUser(userDto);
        return userDto1;
    }


}
