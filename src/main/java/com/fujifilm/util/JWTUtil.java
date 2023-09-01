package com.fujifilm.util;

import java.util.Calendar;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class JWTUtil {

	private JWTUtil() {

	}

	public static String generateToken(String subject, String issuer, String secret, Calendar expiry) {
		// Java JWT Token
		return Jwts.builder()
				// # This is username
				.setSubject(subject.toLowerCase())
				// # Who issue the token
				.setIssuer(issuer)
				// # When token has been issued ?
				.setIssuedAt(new Date(System.currentTimeMillis()))
				// # WHen token will be expired
				.setExpiration(expiry.getTime())
				// # Which signature algorithm, We have used with secret key
				.signWith(io.jsonwebtoken.SignatureAlgorithm.HS512, secret.getBytes()).compact();
	}

	// Read subject/username
	public static String getUserName(String token, String secret) throws Exception {
		return getClaims(token, secret).getSubject();
	}

	public static String getEmailFromCustomClaim(String token, String secret, String claimKey) {

		var claims = Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(token).getBody();
		return claims.get(claimKey).toString();
	}

	// Read Expiration Date
	public static Date getTokenExpiration(String token, String secret) throws Exception {
		return getClaims(token, secret).getExpiration();
	}

	// Read claims
	public static Claims getClaims(String token, String secret) throws Exception {
		return Jwts.parser().setSigningKey(secret.getBytes()).parseClaimsJws(token).getBody();
	}
}
