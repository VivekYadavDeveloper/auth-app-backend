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

    /*CREATE USER API CONTROLLER*/
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto userDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userServices.createUser(userDto));
    }

    /*GET ALL USER API CONTROLLER*/
    @GetMapping
    public ResponseEntity<Iterable<UserDto>> getAllUsers() {
        return ResponseEntity.ok(userServices.getAllUsers());
    }

    /*GET USER BY EMAIL ID API CONTROLLER*/
    @GetMapping("/email/{emailID}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String emailID) {
        return ResponseEntity.ok(userServices.getUserByEmail(emailID));
    }

    /*GET USER BY ID API CONTROLLER*/
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String userId) {
        return ResponseEntity.ok(userServices.getUserById(userId));
    }


    /*DELETE USER API CONTROLLER*/
    /*api/v1/users/{userId}*/
    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable String userId) {
        userServices.deleteUser(userId);
    }

    /*UPDATE USER API CONTROLLER*/
    @PutMapping("/{userId}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String userId, @RequestBody UserDto userDto) {
        return ResponseEntity.ok(userServices.updateUser(userDto, userId));
    }
}
