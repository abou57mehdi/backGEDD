package com.smartged.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

	private final Key signingKey;
	private final long accessTokenTtlMs;

	public JwtService(
			@Value("${app.security.jwt.secret}") String base64Secret,
			@Value("${app.security.jwt.access-ttl-ms:3600000}") long accessTokenTtlMs
	) {
		byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
		this.signingKey = Keys.hmacShaKeyFor(keyBytes);
		this.accessTokenTtlMs = accessTokenTtlMs;
	}

	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	private Claims extractAllClaims(String token) {
		return Jwts.parserBuilder()
				.setSigningKey(signingKey)
				.build()
				.parseClaimsJws(token)
				.getBody();
	}

	public String generateToken(String username, Map<String, Object> extraClaims) {
		long now = System.currentTimeMillis();
		return Jwts.builder()
				.setClaims(extraClaims)
				.setSubject(username)
				.setIssuedAt(new Date(now))
				.setExpiration(new Date(now + accessTokenTtlMs))
				.signWith(signingKey, SignatureAlgorithm.HS256)
				.compact();
	}

	public boolean isTokenValid(String token, String username) {
		final String extracted = extractUsername(token);
		return extracted.equals(username) && !isTokenExpired(token);
	}

	private boolean isTokenExpired(String token) {
		return extractExpiration(token).before(new Date());
	}

	private Date extractExpiration(String token) {
		return extractClaim(token, Claims::getExpiration);
	}
}


