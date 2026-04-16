package com.creationix.auth;

import com.creationix.auth.Config.AppConstants;
import com.creationix.auth.Entities.Role;
import com.creationix.auth.Repositories.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class AuthAppBackendApplication implements CommandLineRunner {
    @Autowired
    private RoleRepository roleRepository;

    public static void main(String[] args) {
        SpringApplication.run(AuthAppBackendApplication.class, args);

    }

    @Override
    public void run(String... args) throws Exception {
        /*WE WILL CREATE SOME DEFAULT USER ROLES*/


        /*ADMIN*/
        roleRepository.findByName("ROLE_" + AppConstants.ADMIN_ROLE).ifPresentOrElse(role -> {
            System.out.println("Admin role already exists:" + role.getName());
        }, () -> {
            Role role = new Role();
            role.setName("ROLE_" + AppConstants.ADMIN_ROLE);
            role.setId(UUID.randomUUID());
            roleRepository.save(role);
        });

        /*GUEST*/
        roleRepository.findByName("ROLE_" + AppConstants.GUEST_ROLE).ifPresentOrElse(role -> {
            System.out.println("Guest role already exists:" + role.getName());
        }, () -> {
            Role role = new Role();
            role.setName("ROLE_" + AppConstants.GUEST_ROLE);
            role.setId(UUID.randomUUID());
            roleRepository.save(role);
        });
    }
}
