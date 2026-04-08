package com.creationix.auth.Security;

import com.creationix.auth.Helper.UserHelper;
import com.creationix.auth.Repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        logger.info("Authorization header : {}", header);

        if (header != null && header.startsWith("Bearer ")) {

            /*TOKEN EXTRACTION AND VALIDATION THEN AUTHENTICATION CREATED*/
            String token = header.substring(7);


            logger.info("Access token found");
            logger.debug("Header Token : {}", token);

            try {
                /*CHECK FOR ACCESS TOKEN HERE*/
                if (!jwtService.isAccessToken(token)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                Jws<Claims> parsed = jwtService.parse(token);
                Claims payload = parsed.getPayload();


                String userID = payload.getSubject();
                logger.debug("Authenticated user with ID: " + userID);

                UUID uuid = UserHelper.parseUUID(userID);
                logger.debug("Parsed UUID: " + uuid);

                if (uuid != null) {
                    userRepository.findById(uuid).ifPresent(user -> {

                        if (user.isEnabled()) {

                            List<GrantedAuthority> authorityList = user.getRoles() == null ? List.of() :
                                    user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
                            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                    user.getEmail(),
                                    null,
                                    authorityList
                            );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                            /*THIS IS THE FINAL LINE TO SET THE AUTHENTICATION CONTEXT WHICH IS IMPORTANT*/
                            if (SecurityContextHolder.getContext().getAuthentication() == null)
                                SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    });
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute("error", "Token is expired");
//                e.printStackTrace();
            } catch (Exception e) {
                request.setAttribute("error", "Invalid token");
//                e.printStackTrace();
            }

        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/login") || path.startsWith("/api/v1/auth/register");
    }

}
