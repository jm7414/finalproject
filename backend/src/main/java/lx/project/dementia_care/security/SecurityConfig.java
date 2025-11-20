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

    // Frontend URL 생성
    private String getFrontendUrl() {
        if (domain.contains("localhost")) {
            return "https://" + domain;
        } else {
            return "https://" + domain;
        }
    }

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

                        // 환자 정보 조회 API (보호자는 연결된 환자 조회, 환자는 자신 조회)
                        .requestMatchers("/api/user/my-patient").hasAnyRole("GUARDIAN", "PATIENT", "SUBSCRIBER")

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
                        .loginPage("https://localhost:5173/login")
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
        // 환경변수에서 도메인 가져오기, 없으면 localhost 사용
        String frontendUrl = getFrontendUrl();
        String productionDomain = domain.contains("localhost") ? "https://lx12mammamia.xyz" : "https://" + domain;
        String productionDomainWww = domain.contains("localhost") ? "https://www.lx12mammamia.xyz" : "https://www." + domain;
        
        corsConfiguration.setAllowedOrigins(Arrays.asList(
                frontendUrl,
                productionDomain,
                productionDomainWww));
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