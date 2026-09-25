package com.Lucca.Projeto1.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TENTATIVAS = 5;
    private static final Duration JANELA = Duration.ofMinutes(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final ApiSecurityErrorWriter errorWriter;

    public LoginRateLimitFilter(ApiSecurityErrorWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    /**
     * Cria um novo bucket para um IP.
     *
     * Cada IP recebe 5 tentativas.
     * Os 5 tokens são repostos a cada 1 minuto.
     */
    private Bucket criarBucket() {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(MAX_TENTATIVAS)
                        .refillIntervally(
                                MAX_TENTATIVAS,
                                JANELA
                        )
                )
                .build();
    }

    /**
     * Define em quais requisições este filtro deve atuar.
     *
     * O rate limit será aplicado somente em:
     *
     * POST /auth/login
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        boolean ehLogin =
                "POST".equalsIgnoreCase(request.getMethod())
                        && "/auth/login".equals(request.getServletPath());

        return !ehLogin;
    }

    /**
     * Executado antes de a requisição chegar ao controller.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String ip = request.getRemoteAddr();

        /*
         * Se esse IP ainda não possui um bucket,
         * cria um automaticamente.
         */
        Bucket bucket = buckets.computeIfAbsent(
                ip,
                chave -> criarBucket()
        );

        /*
         * Cada tentativa de login consome 1 token.
         */
        if (bucket.tryConsume(1)) {

            filterChain.doFilter(request, response);
            return;
        }

        /*
         * Se não houver mais tokens disponíveis,
         * bloqueia a requisição antes que ela chegue
         * ao AuthController.
         */
        errorWriter.write(
                response,
                HttpStatus.TOO_MANY_REQUESTS,
                "Muitas tentativas de login. Tente novamente em alguns instantes."
        );
    }
}