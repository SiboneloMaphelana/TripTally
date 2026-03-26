package com.triptally.security;

import com.triptally.config.TriptallyProperties;
import com.triptally.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

	private final TriptallyProperties properties;

	public JwtService(TriptallyProperties properties) {
		this.properties = properties;
	}

	public String generateToken(User user) {
		Instant now = Instant.now();
		Instant exp = now.plusMillis(properties.getJwt().getExpirationMs());
		return Jwts.builder()
				.subject(user.getEmail())
				.claim("uid", user.getId())
				.issuedAt(Date.from(now))
				.expiration(Date.from(exp))
				.signWith(signingKey())
				.compact();
	}

	public String extractEmail(String token) {
		return parseClaims(token).getSubject();
	}

	public Claims parseClaims(String token) {
		return Jwts.parser()
				.verifyWith(signingKey())
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	private SecretKey signingKey() {
		byte[] keyBytes = properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
		return Keys.hmacShaKeyFor(keyBytes);
	}
}
