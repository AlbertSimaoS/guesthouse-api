
package br.ifsp.guesthouse.config;

import br.ifsp.guesthouse.repository.UserAccountRepository;
import br.ifsp.guesthouse.exception.RestAuthenticationEntryPoint;
import br.ifsp.guesthouse.service.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class    SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            UserAccountRepository userRepository,
            RestAuthenticationEntryPoint authenticationEntryPoint
    ) {
        return new JwtAuthenticationFilter(jwtService, userRepository, authenticationEntryPoint);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                HttpMethod.POST,
                                "/auth/register",
                                "/auth/login",
                                "/auth/forgot-password",
                                "/auth/reset-password"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/room-types/availability"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/reservation/my-reservations"
                        ).hasRole("GUEST")

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/room-types/register",
                                "/room/register",
                                "/season/register",
                                "/reservations/*/consumptions"
                        ).hasRole("RECEPTION")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/reservation"
                        ).hasRole("GUEST")

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/payments/*",
                                "/room/*/update-status",
                                "/reservation/expired",
                                "/reservation/*/cancel",
                                "/reservation/*/checkin",
                                "/reservation/*/checkout"
                        ).hasRole("RECEPTION")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/report/occupancy",
                                "/report/revenue",
                                "/report/daily-movements"
                        ).hasRole("RECEPTION")

                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}