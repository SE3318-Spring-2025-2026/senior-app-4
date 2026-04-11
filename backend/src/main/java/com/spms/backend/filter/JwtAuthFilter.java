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

    private final JwtValidationService jwtValidationService;

    public JwtAuthFilter(JwtValidationService jwtValidationService) {
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

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
        request.setAttribute("jwt_userId", claims.get("userId"));
        request.setAttribute("jwt_role", claims.get("role"));

        filterChain.doFilter(request, response);
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }
}
