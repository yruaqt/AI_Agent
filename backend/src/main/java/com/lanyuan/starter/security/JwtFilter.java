package com.lanyuan.starter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lanyuan.starter.common.api.ApiResponse;
import com.lanyuan.starter.common.exception.ErrorCode;
import com.lanyuan.starter.orchard.EnabledStatus;
import com.lanyuan.starter.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public JwtFilter(JwtService jwtService, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull  HttpServletResponse response,
                                    @NonNull  FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.parse(token);
                String username = claims.getSubject();

                var userOpt = userRepository.findByUsernameAndDeletedFalse(username);
                if (userOpt.isEmpty()) {
                    writeError(response, ErrorCode.UNAUTHORIZED, "用户不存在");
                    return;
                }

                var user = userOpt.get();
                if (user.getStatus() != EnabledStatus.ENABLED) {
                    writeError(response, ErrorCode.UNAUTHORIZED, "账号已停用");
                    return;
                }

                var auth = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (ExpiredJwtException e) {
                log.warn("JWT令牌已过期: {}", e.getMessage());
                writeError(response, ErrorCode.UNAUTHORIZED, "登录令牌已过期，请重新登录");
                return;
            } catch (JwtException e) {
                log.warn("JWT令牌无效: {}", e.getMessage());
                writeError(response, ErrorCode.UNAUTHORIZED, "登录令牌无效，请重新登录");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        SecurityContextHolder.clearContext();
        response.setStatus(errorCode.status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.failure(errorCode.code, message, null);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
