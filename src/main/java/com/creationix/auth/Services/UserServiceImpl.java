package com.creationix.auth.Services;

import com.creationix.auth.Dto.UserDto;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserServices {
    @Override
    public UserDto createUser(UserDto userDto) {
        return null;
    }

    @Override
    public UserDto getUserByEmail(String email) {
        return null;
    }

    @Override
    public UserDto getUserById(String userId) {
        return null;
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        return null;
    }

    @Override
    public void deleteUser(String userId) {

    }

    @Override
    public Iterable<UserDto> getAllUsers() {
        return null;
    }
}
