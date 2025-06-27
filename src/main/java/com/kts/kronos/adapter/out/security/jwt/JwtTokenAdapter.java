package com.kts.kronos.adapter.out.security.jwt;

import com.kts.kronos.domain.port.out.TokenPort;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
@Component
public class JwtTokenAdapter implements TokenPort {

    @Value("${jwt.secret}") private String secret;
    @Value("${jwt.expiration}") private long validityMs;
    @Value("${jwt.refreshExpiration}") private long refreshValidityMs;
    private Key key;

    @PostConstruct
    public void init(){
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }
    @Override
    public String generateToken(Authentication authentication) {
        Date now =  new Date();
        return Jwts.builder()
                .setSubject(authentication.getName())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + validityMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public String generateRefreshToken(Authentication authentication) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(authentication.getName())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + refreshValidityMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public Authentication parseToken(String token) {
      Claims claims =  Jwts.parserBuilder().setSigningKey(key).build()
              .parseClaimsJws(token).getBody();
      return new UsernamePasswordAuthenticationToken(
              claims.getSubject(),null, List.of()
      );
    }
}
