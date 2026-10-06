package com.argos.identity;

import com.argos.shared.security.AuthenticatedUser;
import com.argos.shared.web.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Autentica pelo header Authorization: Bearer. Instanciado manualmente na SecurityConfig (não é @Component)
 * para não ser registrado duas vezes na cadeia de filtros do servlet.
 */
class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;

    JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            jwtService.parse(header.substring(PREFIX.length()).trim()).ifPresent(this::authenticate);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TraceContext.USER_ID);
        }
    }

    private void authenticate(AuthenticatedUser user) {
        var authentication = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        MDC.put(TraceContext.USER_ID, user.userId().toString());
    }
}
