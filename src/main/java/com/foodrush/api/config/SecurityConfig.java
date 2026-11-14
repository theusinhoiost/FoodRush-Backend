package com.foodrush.api.config;

import com.foodrush.api.security.JwtAuthenticationFilter;
import com.foodrush.api.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  @Value("${cors.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*,https://food-rush-web.vercel.app,https://*.vercel.app,https://foodrush-backend-production-b263bc5d.up.railway.app}")
  private String allowedOriginPatterns;

  private final JwtTokenProvider tokenProvider;
  private final UserDetailsService userDetailsService;

  public SecurityConfig(
    JwtTokenProvider tokenProvider,
    UserDetailsService userDetailsService
  ) {
    this.tokenProvider = tokenProvider;
    this.userDetailsService = userDetailsService;
  }

  @Bean
  public JwtAuthenticationFilter jwtAuthenticationFilter() {
    return new JwtAuthenticationFilter(tokenProvider, userDetailsService);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(
    AuthenticationConfiguration authConfig
  ) throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    List<String> patterns = Arrays.stream(allowedOriginPatterns.split(","))
      .map(String::trim)
      .toList();
    configuration.setAllowedOriginPatterns(patterns);
    configuration.setAllowedMethods(
      List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
    );
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source =
      new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> {
      response.setStatus(HttpStatus.UNAUTHORIZED.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");

      ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.UNAUTHORIZED,
        "Token de autenticação ausente ou inválido. Faça login para acessar este recurso."
      );
      problemDetail.setTitle("Não Autorizado");
      problemDetail.setType(URI.create("https://api.foodrush.com/errors/unauthorized"));

      new ObjectMapper().writeValue(response.getOutputStream(), problemDetail);
    };
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setStatus(HttpStatus.FORBIDDEN.value());
      response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
      response.setCharacterEncoding("UTF-8");

      ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.FORBIDDEN,
        "Você não possui o papel ou permissão necessária para acessar este recurso."
      );
      problemDetail.setTitle("Acesso Negado");
      problemDetail.setType(URI.create("https://api.foodrush.com/errors/forbidden"));

      new ObjectMapper().writeValue(response.getOutputStream(), problemDetail);
    };
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http)
    throws Exception {
    http
      .cors(Customizer.withDefaults())
      .csrf(csrf -> csrf.disable())
      .exceptionHandling(exceptions ->
        exceptions
          .authenticationEntryPoint(authenticationEntryPoint())
          .accessDeniedHandler(accessDeniedHandler())
      )
      .sessionManagement(session ->
        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
      )
      .authorizeHttpRequests(auth ->
        auth
          // 1. Rotas públicas de autenticação e webhooks de pagamento
          .requestMatchers(
            "/auth/**",
            "/api/v1/auth/**",
            "/error",
            "/payments/webhook",
            "/api/v1/payments/webhook"
          )
          .permitAll()
          // 2. Consulta pública de restaurantes, cardápios e geocodificação de endereços
          .requestMatchers(
            HttpMethod.GET,
            "/restaurants/**",
            "/api/v1/restaurants/**",
            "/menu-items/**",
            "/api/v1/menu-items/**"
          )
          .permitAll()
          .requestMatchers(
            HttpMethod.POST,
            "/addresses/geocode",
            "/api/v1/addresses/geocode"
          )
          .permitAll()
          // 3. Liberações do Swagger / OpenAPI
          .requestMatchers(
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/api/v1/docs/**",
            "/docs/**"
          )
          .permitAll()
          // 4. Endpoints de métricas e saúde (Actuator / Prometheus)
          .requestMatchers("/actuator/**", "/api/v1/actuator/**")
          .permitAll()
          // 5. Gestão de Restaurantes e Cardápios (Restaurante ou Admin)
          .requestMatchers(
            HttpMethod.POST,
            "/restaurants/**",
            "/api/v1/restaurants/**"
          )
          .hasAnyRole("RESTAURANT", "ADMIN")
          .requestMatchers(
            HttpMethod.PUT,
            "/restaurants/**",
            "/api/v1/restaurants/**"
          )
          .hasAnyRole("RESTAURANT", "ADMIN")
          .requestMatchers(
            HttpMethod.PATCH,
            "/restaurants/**",
            "/api/v1/restaurants/**",
            "/menu-items/**",
            "/api/v1/menu-items/**"
          )
          .hasAnyRole("RESTAURANT", "ADMIN")
          .requestMatchers(
            HttpMethod.DELETE,
            "/menu-items/**",
            "/api/v1/menu-items/**"
          )
          .hasAnyRole("RESTAURANT", "ADMIN")
          // 6. Fluxo de Pedidos (Criar pedido = CLIENT / Atualizar status = RESTAURANT, COURIER, ADMIN)
          .requestMatchers(HttpMethod.POST, "/orders/**", "/api/v1/orders/**")
          .hasRole("CLIENT")
          .requestMatchers(HttpMethod.PATCH, "/orders/**", "/api/v1/orders/**")
          .hasAnyRole("RESTAURANT", "COURIER", "ADMIN")
          // 7. Qualquer outra rota exige estar autenticado
          .anyRequest()
          .authenticated()
      )
      .addFilterBefore(
        jwtAuthenticationFilter(),
        UsernamePasswordAuthenticationFilter.class
      );

    return http.build();
  }
}
