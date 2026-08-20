package api.poja.app.security;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@AllArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final CustomUserDetailsService userDetailsService;
  private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .exceptionHandling(
            handling ->
                handling
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                    .accessDeniedHandler(jwtAccessDeniedHandler))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authenticationProvider(authenticationProvider())
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    .requestMatchers("/ping", "/health/**")
                    .permitAll()
                    .requestMatchers(
                        "/login",
                        "/api/v1/auth/login",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/favicon.ico")
                    .permitAll()
                    .requestMatchers("/api/v1/users/**")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/academic-years")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/courses")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/course-assignments/*/exams")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/groups")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/promotions")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/promotions/*/groups/*")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/course-assignments")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/course-assignments/*/teachers/*")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/students/*/promotion-and-group")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.PATCH, "/api/v1/students/*/group")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/students/*/transcripts/*/generate")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.POST, "/api/v1/students/*/transcripts/*/send")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.GET, "/api/v1/students/*/graduation")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/v1/promotions/*/graduates")
                    .hasRole("ADM")
                    .requestMatchers(HttpMethod.GET, "/api/v1/promotions/*/graduates/export")
                    .hasRole("ADM")
                    .requestMatchers("/api/v1/teachers/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    var provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder());
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
      throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
