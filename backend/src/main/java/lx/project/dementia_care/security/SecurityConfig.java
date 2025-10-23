package lx.project.dementia_care.security;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lx.project.dementia_care.service.CustomUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedPage("/login"))
                .authorizeHttpRequests(authz -> authz
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR)
                        .permitAll()

                        // 회원가입, 로그인 페이지 접근 허용
                        .requestMatchers("/api/login", "/api/logout", "/api/register", "/SignUp").permitAll()
                        .requestMatchers("/api/user/check-duplicate").permitAll()
                        .requestMatchers("/api/route/**").permitAll()

                        // 업로드는 로그인 필요 (세션)
                        .requestMatchers("/api/upload/**").authenticated()

                        // 정적 리소스
                        .requestMatchers("/index.html", "/favicon.ico", "/assets/**", "/images/**").permitAll()

                        // 보호자/구독자 전용 페이지 및 API
                        .requestMatchers("/GD", "/api/guardian/**", "/api/posts/**", "/api/missing-posts/**")
                        .hasAnyRole("GUARDIAN", "SUBSCRIBER")

                        // 일정 관리 API (보호자/구독자 전용)
                        .requestMatchers("/api/schedule/**").hasAnyRole("GUARDIAN", "SUBSCRIBER")

                        // 보호자가 관리하는 환자 조회 API
                        .requestMatchers("/api/user/my-patient").hasAnyRole("GUARDIAN", "SUBSCRIBER")

                        // 환자 전용 페이지 및 API
                        .requestMatchers("/DP", "/api/patient/**").hasRole("PATIENT")

                        // 본인정보 수정 API 권한
                        .requestMatchers(HttpMethod.POST, "/api/user/update").hasAnyRole("GUARDIAN","PATIENT", "SUBSCRIBER")

                        // 공통 페이지들 (로그인한 사용자만 접근 가능)
                        .requestMatchers("/calendar", "/add-schedule", "/CommunityView", "/geo-fencing",
                                "/search-route", "/predict-location", "/total-support", "/money-support",
                                "/record", "/report", "/gdmypage", "/dpmypage", "/basicplan", "/plusplan")
                        .authenticated()

                        .requestMatchers("/api/user/me").authenticated()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .formLogin(login -> login
                        .loginPage("https://localhost:5173/login") // Vue 로그인 페이지
                        .loginProcessingUrl("/api/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler((request, response, exception) -> response.setStatus(401))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(200))
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                HttpSession session = request.getSession();
                boolean isGuardianOrSubscriber = authentication.getAuthorities().stream()
                        .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_GUARDIAN") ||
                                grantedAuthority.getAuthority().equals("ROLE_SUBSCRIBER"));

                if (isGuardianOrSubscriber) {
                    session.setAttribute("GUARDIAN", true);
                } else {
                    session.setAttribute("PATIENT", true);
                }

                session.setAttribute("username", authentication.getName());
                session.setAttribute("isAuthenticated", true);
                response.setStatus(200);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(Arrays.asList(
                "https://localhost:5173",
                "https://lx12mammamia.xyz",
                "https://www.lx12mammamia.xyz"));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"));
        corsConfiguration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}