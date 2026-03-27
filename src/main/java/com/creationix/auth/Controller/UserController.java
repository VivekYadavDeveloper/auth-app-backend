package com.creationix.auth.Controller;

import com.creationix.auth.Dto.UserDto;
import com.creationix.auth.Services.UserServices;
import lombok.AllArgsConstructor;
import org.hibernate.boot.internal.Abstract;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@AllArgsConstructor
public class UserController {

    private final UserServices userServices;

    /*CREATE USER API*/
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userServices.createUser(userDto));
    }

    /*GET ALL USER API*/
    @GetMapping
    public ResponseEntity<Iterable<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userServices.getAllUsers());
    }
}
