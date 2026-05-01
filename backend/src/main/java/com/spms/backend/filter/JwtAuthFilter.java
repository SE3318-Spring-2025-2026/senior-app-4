package com.spms.backend.filter;

import com.spms.backend.service.JwtValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    // Bu path'ler token gerektirmez
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/students/validate",
            "/api/v1/students/ids/upload",
            "/api/v1/auth/github",
            "/api/v1/auth/github/callback",
            "/api/v1/auth/login",
            "/api/v1/auth/change-password",
            "/api/v1/auth/reset-password",
            "/api/v1/professors/register"
    );

    private static final Set<String> PUBLIC_PREFIXES = Set.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/swagger-resources",
            "/webjars"
    );

    private final JwtValidationService jwtValidationService;

    public JwtAuthFilter(JwtValidationService jwtValidationService) {
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        // --- YENİ EKLENEN KISIM: CORS ÖN KONTROLÜ (PREFLIGHT) ---
        // OPTIONS isteklerini Security/Token filtresinden geçirmez, CORS confignize gönderir.
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }
        // --------------------------------------------------------

        String path = request.getRequestURI();

        if (isPublic(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Authorization token is required.");
            return;
        }

        String token = authHeader.substring(7);
        Map<String, Object> claims = jwtValidationService.validateAndParse(token);

        if (claims == null) {
            sendUnauthorized(response, "Invalid or expired token.");
            return;
        }

        // Claims'i request attribute olarak sakla (controller'lardan erişilebilir)
        request.setAttribute("jwt_claims", claims);

        // Rol kontrolü — null/boş rol varsa token hatalı sayılır
        Object userId = claims.get("userId");
        Object role   = claims.get("role");
        if (userId == null || role == null || role.toString().isBlank()) {
            sendUnauthorized(response, "Token is missing required claims (userId or role).");
            return;
        }

        request.setAttribute("jwt_userId", userId);
        request.setAttribute("jwt_role", role);

        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        if (PUBLIC_PATHS.contains(path)) return true;
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}
