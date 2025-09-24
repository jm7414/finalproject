package lx.project.dementia_care.security;

import java.io.IOException;
import java.util.Arrays;

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

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{
        
        http
	        .exceptionHandling(exceptionHandling -> exceptionHandling.accessDeniedPage("/login"))
	        .authorizeHttpRequests(authz -> authz
		        .dispatcherTypeMatchers(DispatcherType.FORWARD,DispatcherType.INCLUDE,DispatcherType.ERROR).permitAll()
		        .requestMatchers("/login").permitAll()
		        .requestMatchers("/index.html", "/favicon.ico", "/assets/**").permitAll()
		        .requestMatchers(HttpMethod.GET,"/login","/logout","/register-page","/notice-check-page","/menu/all").permitAll()
                .requestMatchers(HttpMethod.GET,"/chat/{senderNo}/last-messages","/chat/{roomId}/messages","/user/me").authenticated()
		        .requestMatchers(HttpMethod.GET,"/notice-add-page","/notice-modify-page").hasAuthority("ADMIN")
		        .requestMatchers(HttpMethod.POST,"/login","/register").permitAll()
                .requestMatchers(HttpMethod.GET,"/admin").hasAuthority("ADMIN")
		        .requestMatchers(HttpMethod.POST,"/menu/add", "/admin").hasAuthority("ADMIN")
		        .requestMatchers(HttpMethod.PUT,"/menu/update", "/admin").hasAuthority("ADMIN")
		        .requestMatchers(HttpMethod.DELETE,"/menu/delete", "/admin").hasAuthority("ADMIN")
		        .anyRequest().authenticated()
	        )

	        .csrf(csrf -> csrf.disable())
	        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
	        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
	        
	        .formLogin(login -> login
	        	// 로그인 페이지 구현되면 해제 하기
		        //.loginPage("/login")
		        .failureUrl("/login?error=true")
		        .usernameParameter("username")
		        .passwordParameter("password")
		        .successHandler(authenticationSuccessHandler())
		        .permitAll()
	        )
	        
	        .logout(logout->logout
                .logoutRequestMatcher(new org.springframework.security.web.util.matcher.AntPathRequestMatcher("/logout"))
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );
        return http.build();
    }
    
    @Bean
    public AuthenticationSuccessHandler authenticationSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                    Authentication authentication) throws IOException, ServletException {
                HttpSession session = request.getSession();
                boolean isAdmin = authentication.getAuthorities().stream().anyMatch(
                        grantedAuthority ->  grantedAuthority.getAuthority().equals("ADMIN"));
                
                if(isAdmin) {
                    session.setAttribute("ADMIN", true);
                } else {
                    session.setAttribute("MEMBER", true);
                }
                
                session.setAttribute("username", authentication.getName());
                session.setAttribute("isAuthenticated",true);
                // 인증 성공 시 Vue 앱으로 리다이렉트
                response.sendRedirect("http://localhost:5173/");
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.setAllowedOrigins(Arrays.asList(
                // Vue 개발 서버 포트 추가
                "http://localhost:5173","https://localhost:5173",
                // 기존 Spring 포트 유지 (필요시)
                "http://localhost:8080","https://localhost:8080"
        ));
        corsConfiguration.setAllowedHeaders(Arrays.asList(
        		// Vue에서 인증 토큰이나 추가 헤더를 보낼 가능성 고려
        	    "Authorization","Content-Type","Accept","Origin","X-Requested-With"
        	));
        corsConfiguration.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE"));
        corsConfiguration.setAllowedHeaders(Arrays.asList("Authorization","Content-Type"));
        corsConfiguration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
    }
    // 비밀번호 해쉬화
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder();
//    }
}
