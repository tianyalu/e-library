package com.example.elibrary.infrastructure.security;

import com.example.elibrary.api.error.SecurityProblemHandlers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 无状态 JSON API 的安全配置。三条规则：
 * <ul>
 *   <li>不用 Session / Cookie，CSRF 没有着力点，横向扩容也不用管会话粘滞；</li>
 *   <li>书目浏览公开，其它都要带令牌；</li>
 *   <li>运维端点额外要求 LIBRARIAN 角色——但"这条记录是不是你的"这层检查在应用层，
 *       不能只靠这里的角色。</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           SecurityProblemHandlers problemHandlers) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(handling -> handling
                    .authenticationEntryPoint(problemHandlers)
                    .accessDeniedHandler(problemHandlers))
            .authorizeHttpRequests(requests -> requests
                    .requestMatchers(HttpMethod.GET, "/api/v1/books", "/api/v1/books/**").permitAll()
                    .requestMatchers("/api/v1/auth/**",
                            "/actuator/**",
                            "/v3/api-docs/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html").permitAll()
                    .requestMatchers("/api/v1/ops/**").hasRole("LIBRARIAN")
                    .anyRequest().authenticated())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
