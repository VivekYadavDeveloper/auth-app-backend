package com.creationix.auth.Services.Impls;

import com.creationix.auth.Dto.UserDto;
import com.creationix.auth.Entities.Provider;
import com.creationix.auth.Entities.User;
import com.creationix.auth.Exception.ResourceNotFoundException;
import com.creationix.auth.Helper.UserHelper;
import com.creationix.auth.Repositories.UserRepository;
import com.creationix.auth.Services.UserServices;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserServices {

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email cannot be blank");
        }
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new IllegalArgumentException("User with this email already exists");
        }

        /*Convert DTO to Object*/
        User user = modelMapper.map(userDto, User.class);
        user.setProvider(userDto.getProvider() != null ? userDto.getProvider() : Provider.LOCAL);

        /*Role assign here to user for authorization*/
        /*TODO: Assign Here*/
        User savedUser = userRepository.save(user);
        return modelMapper.map(savedUser, UserDto.class);
    }

    @Override
    public UserDto getUserByEmail(String email) {

        User user = userRepository.findByEmail(email).
                orElseThrow(() -> new ResourceNotFoundException("User not found with given: " + email + "email id"));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository
                .findById(UserHelper.parseUUID(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return modelMapper.map(user, UserDto.class);
    }

    @Override
    public UserDto updateUser(UserDto userDto, String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User exsistingUser = userRepository
                .findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        if (userDto.getName() != null) {
            exsistingUser.setName(userDto.getName());
        }
        if (userDto.getImage() != null) {
            exsistingUser.setImage(userDto.getImage());
        }
        if (userDto.getProvider() != null) {
            exsistingUser.setProvider(userDto.getProvider());
        }
        /*TODO: CHANGE THE PASSWORD UPDATION LOGIC...*/
        if (userDto.getPassword() != null) {
            exsistingUser.setPassword(userDto.getPassword());
        }
        exsistingUser.setEnabled(userDto.isEnabled());
        User updatedUser = userRepository.save(exsistingUser);
        return modelMapper.map(updatedUser, UserDto.class);
    }

    @Override
    public void deleteUser(String userId) {
        UUID uId = UserHelper.parseUUID(userId);
        User user = userRepository
                .findById(uId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public Iterable<UserDto> getAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
    }
}
