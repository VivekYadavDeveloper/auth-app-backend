package com.creationix.auth.Services;

import com.creationix.auth.Dto.UserDto;

import java.util.Iterator;
import java.util.UUID;

public interface UserServices {
    UserDto createUser(UserDto userDto);

    UserDto getUserByEmail(String email);

    UserDto getUserById(String userId);

    UserDto updateUser(UserDto userDto, String userId);

    void deleteUser(String userId);

    Iterable<UserDto> getAllUsers();
}
