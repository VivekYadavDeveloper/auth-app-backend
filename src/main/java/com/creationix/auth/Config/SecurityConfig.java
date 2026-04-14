package com.creationix.auth.Config;

import com.creationix.auth.Dto.ApiError;
import com.creationix.auth.Security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

import static com.creationix.auth.Config.AppConstants.AUTH_PUBLIC_URLS;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final AuthenticationSuccessHandler successHandler;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AuthenticationSuccessHandler successHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.successHandler = successHandler;
    }


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        /*Yeaah Process Kro Yaa Uske Niche wala dono same kaam krega*/
//        http.csrf(AbstractHttpConfigurer::disable);
//        http.authorizeHttpRequests(authorizeRequests ->
//                        authorizeRequests.requestMatchers("/api/v1/auth/register").permitAll()
//                                .requestMatchers("/api/v1/auth/login").permitAll()
//                                .anyRequest().authenticated()
//                )
//                .httpBasic(Customizer.withDefaults());

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(AUTH_PUBLIC_URLS).permitAll()
//                        .requestMatchers("/api/v1/auth/login").permitAll()
//                        .requestMatchers("/api/v1/auth/refresh").permitAll()
//                        .requestMatchers("/api/v1/auth/logout").permitAll()
                        .anyRequest().authenticated())
                .oauth2Login(oAuth2 -> oAuth2.successHandler(successHandler).failureHandler(null)).logout(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception.authenticationEntryPoint(((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    String message = authException.getMessage();

                    String error = (String) request.getAttribute("error");
                    if (error != null) {
                        message = error;
                    }
                    /*37:19*/
//                    Map<String, Object> errorMap = Map.of("message", message, "statusCode", 401);
                    var apiError = ApiError.of(HttpStatus.UNAUTHORIZED.value(), request.getRequestURI(), "Unauthorized Access", message);
                    var objectMapper = new ObjectMapper();
                    response.getWriter().write(objectMapper.writeValueAsString(apiError));
                }))).addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) {
        return configuration.getAuthenticationManager();
    }

/*    @Bean
    public UserDetailsService user(){
        User.UserBuilder userBuilder = User.withDefaultPasswordEncoder();
        UserDetails user1 = userBuilder.username("Vivek").password("Vivek123").roles("ADMIN").build();
        UserDetails user2 = userBuilder.username("Varsha").password("Varsha123").roles("USER").build();
        return new InMemoryUserDetailsManager(user1,user2);
    }*/


}
