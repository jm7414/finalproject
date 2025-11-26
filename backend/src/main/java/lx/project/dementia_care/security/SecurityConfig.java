package lx.project.dementia_care.security;

import java.io.IOException;
import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    // 환경변수에서 도메인 가져오기, 없으면 localhost:5173 사용 (개발용)
    @Value("${DOMAIN:localhost:5173}")
    private String domain;

    // Frontend URL 생성 (https:// 중복 방지)
    private String getFrontendUrl() {
        if (domain.startsWith("http://") || domain.startsWith("https://")) {
            return domain;
        } else if (domain.contains("localhost")) {
            return "https://" + domain;
        } else {
            return "https://" + domain;
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            // API 요청에 대해서는 JSON 응답 반환
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"message\":\"인증이 필요합니다.\"}");
                            } else {
                                // 일반 페이지 요청은 로그인 페이지로 리다이렉트
                                response.sendRedirect(getFrontendUrl() + "/login");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // API 요청에 대해서는 JSON 응답 반환
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                response.setContentType("application/json;charset=UTF-8");
                                response.getWriter().write("{\"message\":\"접근 권한이 없습니다.\"}");
                            } else {
                                // 일반 페이지 요청은 로그인 페이지로 리다이렉트
                                response.sendRedirect(getFrontendUrl() + "/login");
                            }
                        }))
                .authorizeHttpRequests(authz -> authz
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR)
                        .permitAll()

                        // 회원가입, 로그인 페이지 접근 허용
                        .requestMatchers("/api/login", "/api/logout", "/api/register", "/SignUp").permitAll()
                        .requestMatchers("/api/user/check-duplicate").permitAll()
                        .requestMatchers("/api/route/**").permitAll()

                        // 업로드는 로그인 필요 (세션)
                        .requestMatchers("/api/upload/**").authenticated()

                        // 정적 리소스 (업로드된 이미지 포함)
                        .requestMatchers("/index.html", "/favicon.ico", "/assets/**", "/images/**", "/uploads/**").permitAll()

                        // 보호자/구독자 전용 페이지 및 API
                        .requestMatchers("/GD", "/api/guardian/**", "/api/posts/**", "/api/missing-posts/**")
                        .hasAnyRole("GUARDIAN", "SUBSCRIBER")

                        // 일정 조회 API (환자도 자신의 일정 조회 가능)
                        .requestMatchers(HttpMethod.GET, "/api/schedule/list/**", "/api/schedule/*/locations", 
                                "/api/schedule/*/route", "/api/schedule/safe-zones/**", 
                                "/api/schedule/basic-safe-zone/**", "/api/schedule/current/**", "/api/schedule/*")
                        .hasAnyRole("GUARDIAN", "PATIENT", "SUBSCRIBER")

                        // 일정 생성/수정/삭제 API (보호자/구독자 전용)
                        .requestMatchers("/api/schedule/**").hasAnyRole("GUARDIAN", "SUBSCRIBER")

                        // 위치 업데이트 API (환자/구독자 전용) + 함께찾는사람 때문에 가디언도 추가 함
                        .requestMatchers(HttpMethod.POST, "/api/location/update").hasAnyRole("GUARDIAN", "PATIENT", "SUBSCRIBER")

                        // 지현 추가: 이웃 위치 업데이트 API (이웃/보호자 전용) / 환자에서 이웃으로 들어갔을때 안돼서 추가한 사항
                        .requestMatchers(HttpMethod.POST, "/NH/api/neighbor/location/update")
                        .hasAnyRole("NEIGHBOR", "GUARDIAN", "PATIENT", "SUBSCRIBER")

                        // 위치 조회 API (보호자/구독자 전용)
                        .requestMatchers(HttpMethod.GET, "/api/location/patient/**").hasAnyRole("GUARDIAN", "SUBSCRIBER")

                        // 환자 정보 조회 API (보호자/이웃/환자/구독자: 연결된 환자 조회)
                        .requestMatchers("/api/user/my-patient").hasAnyRole("GUARDIAN", "NEIGHBOR", "PATIENT", "SUBSCRIBER")

                        // 환자 전용 페이지 및 API
                        .requestMatchers("/DP", "/api/patient/**").hasAnyRole("PATIENT", "SUBSCRIBER")

                        // 이웃 페이지 (보호자/이웃/환자/구독자 접근 가능)
                        .requestMatchers("/NH").hasAnyRole("GUARDIAN", "NEIGHBOR", "PATIENT", "SUBSCRIBER")
                        
                        // 이웃 API (보호자/이웃/환자/구독자 접근 가능)
                        .requestMatchers("/NH/api/**").hasAnyRole("GUARDIAN", "NEIGHBOR", "PATIENT", "SUBSCRIBER")

                        // 본인정보 수정 API 권한
                        .requestMatchers(HttpMethod.POST, "/api/user/update").hasAnyRole("GUARDIAN","PATIENT", "SUBSCRIBER", "NEIGHBOR")

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
                        .loginPage(getFrontendUrl() + "/login")
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
        // 환경변수에서 도메인 가져오기
        String frontendUrl = getFrontendUrl();
        
        // 프로덕션 도메인 처리 (https:// 중복 방지)
        String productionDomain;
        String productionDomainWww;
        
        if (domain.contains("localhost")) {
            // 개발 환경: 프로덕션 도메인도 허용
            productionDomain = "https://lx12mammamia.xyz";
            productionDomainWww = "https://www.lx12mammamia.xyz";
        } else {
            // 프로덕션 환경: 환경변수에서 가져온 도메인 사용
            if (domain.startsWith("http://") || domain.startsWith("https://")) {
                productionDomain = domain;
                productionDomainWww = domain.replace("://", "://www.");
            } else {
                productionDomain = "https://" + domain;
                productionDomainWww = "https://www." + domain;
            }
        }
        
        corsConfiguration.setAllowedOrigins(Arrays.asList(
                frontendUrl,
                productionDomain,
                productionDomainWww,
                "https://lx12mammamia.xyz",
                "https://www.lx12mammamia.xyz"));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfiguration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With", "X-CSRF-TOKEN"));
        corsConfiguration.setAllowCredentials(true);
        corsConfiguration.setMaxAge(3600L);
        corsConfiguration.setExposedHeaders(Arrays.asList("Set-Cookie", "JSESSIONID"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}