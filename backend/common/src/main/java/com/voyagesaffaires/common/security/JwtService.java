package com.voyagesaffaires.common.security;

import com.voyagesaffaires.common.enums.DomaineActivite;
import com.voyagesaffaires.common.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Generation (user-service) et validation (tous les services) des jetons JWT.
 * Tous les microservices partagent la meme cle secrete (app.jwt.secret).
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms:86400000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    public String generateToken(AuthUser u) {
        Date now = new Date();
        return Jwts.builder()
                .subject(u.email())
                .claim("uid", u.id())
                .claim("role", u.role().name())
                .claim("nom", u.nomComplet())
                .claim("prenom", u.prenom())
                .claim("eid", u.entrepriseId())
                .claim("enom", u.entrepriseNom())
                .claim("dom", u.domaine() == null ? null : u.domaine().name())
                .claim("poste", u.poste())
                .claim("tel", u.telephone())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /** Retourne l'utilisateur si le jeton est valide (signature + expiration), sinon null. */
    public AuthUser parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String dom = c.get("dom", String.class);
            Number eid = c.get("eid", Number.class);
            return new AuthUser(
                    c.get("uid", Number.class).longValue(),
                    c.getSubject(),
                    Role.valueOf(c.get("role", String.class)),
                    c.get("nom", String.class),
                    c.get("prenom", String.class),
                    eid == null ? null : eid.longValue(),
                    c.get("enom", String.class),
                    dom == null ? null : DomaineActivite.valueOf(dom),
                    c.get("poste", String.class),
                    c.get("tel", String.class));
        } catch (JwtException | IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
