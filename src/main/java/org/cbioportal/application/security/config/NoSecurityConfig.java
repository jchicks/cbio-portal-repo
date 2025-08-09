package org.cbioportal.application.security.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(value = "authenticate", havingValue = "false", matchIfMissing = true)
public class NoSecurityConfig {
  //  @Bean
  //  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  //    return http.cors(Customizer.withDefaults())
  //        .csrf(AbstractHttpConfigurer::disable)
  //        .authorizeHttpRequests(
  //            auth -> auth.requestMatchers("/").permitAll().anyRequest().permitAll())
  //        .build();
  //  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http.cors(
            cors ->
                cors.configurationSource(
                    request -> {
                      var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                      corsConfig.addAllowedOrigin(
                          "http://localhost:3000"); // Allow React app origin
                      corsConfig.addAllowedMethod("*"); // Allow all HTTP methods
                      corsConfig.addAllowedHeader("*"); // Allow all headers
                      corsConfig.setAllowCredentials(
                          true); // Allow sending credentials like cookies
                      return corsConfig;
                    }))
        .csrf(csrf -> csrf.disable()) // Disable CSRF protection
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/")
                    .permitAll()
                    .anyRequest()
                    .permitAll()) // Allow all requests
        .build();
  }
}
