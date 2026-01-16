package com.maximum0.fastpickbe.common.security.provider;

import com.maximum0.fastpickbe.common.exception.BusinessException;
import com.maximum0.fastpickbe.common.exception.ErrorCode;
import com.maximum0.fastpickbe.common.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
/**
 * JWT(JSON Web Token)의 생성, 추출, 유효성 검증을 담당하는 핵심 컴포넌트입니다.
 * Spring Security와 연동하여 인증 객체(Authentication)를 생성하고 관리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private static final String AUTHORITIES_KEY = "auth";
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private SecretKey key;

    /**
     * 설정된 Secret Key를 바탕으로 HMAC-SHA 알고리즘에 사용할 키를 초기화합니다.
     */
    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secretKey().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 인증된 사용자 정보를 바탕으로 Access Token을 생성합니다.
     *
     * @param authentication Spring Security 인증 객체
     * @return 생성된 Access Token 문자열
     */
    public String createAccessToken(Authentication authentication) {
        return createToken(authentication, jwtProperties.accessTokenExpiration().toMillis());
    }

    /**
     * 인증된 사용자 정보를 바탕으로 Refresh Token을 생성합니다.
     *
     * @param authentication Spring Security 인증 객체
     * @return 생성된 Refresh Token 문자열
     */
    public String createRefreshToken(Authentication authentication) {
        return createToken(authentication, jwtProperties.refreshTokenExpiration().toMillis());
    }

    /**
     * JWT 토큰을 복호화하여 Spring Security에서 사용할 인증 객체(Authentication)를 생성합니다.
     *
     * @param token JWT 토큰
     * @return {@link UsernamePasswordAuthenticationToken} 기반의 인증 정보
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);

        if (claims.get(AUTHORITIES_KEY) == null) {
            log.error("JWT 토큰에 권한 정보가 누락되었습니다.");
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /**
     * 토큰의 서명 위조, 만료 여부, 지원하지 않는 형식 등을 검증합니다.
     *
     * @param token 검증할 JWT 토큰
     * @return 유효한 토큰일 경우 true, 그 외 예외 발생 시 false
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.warn("잘못된 JWT 서명입니다: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("지원되지 않는 JWT 토큰입니다: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT 클레임 문자열이 비어 있습니다: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 공통 토큰 생성 메서드입니다.
     *
     * @param authentication 인증 정보
     * @param expirationMillis 만료 시간(ms)
     * @return JWT 토큰 문자열
     */
    private String createToken(Authentication authentication, long expirationMillis) {
        String authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        Date now = new Date(clock.millis());
        Date validity = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(authentication.getName())
                .claim(AUTHORITIES_KEY, authorities)
                .issuedAt(now)
                .expiration(validity)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰을 파싱하여 클레임(Claims) 정보를 추출합니다.
     *
     * @param token JWT 토큰
     * @return 추출된 Claims 객체
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .clock(() -> new Date(clock.millis()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}