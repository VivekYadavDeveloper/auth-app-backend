package com.creationix.auth.Config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auth Application build by Vivek Yadav @Creationix Team.",
                description = "Generic auth app that can be used with any application.",
                contact = @Contact(
                        name = "Vivek",
                        url = "https://yadavjiportfolio.vercel.app/",
                        email = "desiappdev24@gmail.com"
                ),
                version = "1.0",
                summary = "This app is very useful if you dont want create auth app from scratch."


        )
        ,
        security = {
                @SecurityRequirement(
                        name = "bearerAuth"
                )
        }


)

@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer", //Authorization: Bearer htokenaswga,
        bearerFormat = "JWT"

)

public class APIDocConfig {
}
