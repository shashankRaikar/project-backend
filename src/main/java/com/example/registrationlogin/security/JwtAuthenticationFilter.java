package com.example.registrationlogin.security;

import com.example.registrationlogin.authentication.repository.JwtTokenRepository;
import com.example.registrationlogin.entity.JwtToken;
import com.example.registrationlogin.entity.User;
import com.example.registrationlogin.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final JwtTokenRepository jwtTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.cookie-name:authToken}")
    private String cookieName;

    @Autowired
    public JwtAuthenticationFilter(JwtService jwtService,
                                   JwtTokenRepository jwtTokenRepository,
                                   UserRepository userRepository) {
        this.jwtService = jwtService;
        this.jwtTokenRepository = jwtTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        System.out.println("JWT FILTER: " + request.getMethod() + " " + request.getRequestURI());
        String token = jwtService.extractTokenFromCookie(request, cookieName);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String username = jwtService.extractUsername(token);

                if (username != null && jwtService.validateToken(token, username)) {
                    // Check database persistence and database-side expiration
                    Optional<JwtToken> tokenOpt = jwtTokenRepository.findByToken(token);
                    if (tokenOpt.isPresent() && tokenOpt.get().getExpiryTime().isAfter(LocalDateTime.now())) {
                        Optional<User> userOptional = userRepository.findByUsername(username);

                        if (userOptional.isPresent()) {
                            User user = userOptional.get();
                            UsernamePasswordAuthenticationToken authToken =
                                    new UsernamePasswordAuthenticationToken(
                                            user,
                                            null,
                                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                    );
                            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authToken);
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("Could not authenticate user from JWT token: " + e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
