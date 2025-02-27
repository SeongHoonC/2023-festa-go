package com.festago.entry.infrastructure;

import com.festago.common.exception.BadRequestException;
import com.festago.common.exception.ErrorCode;
import com.festago.entry.application.EntryCodeExtractor;
import com.festago.entry.domain.EntryCodePayload;
import com.festago.ticketing.domain.EntryState;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

public class JwtEntryCodeExtractor implements EntryCodeExtractor {

    private static final String MEMBER_TICKET_ID_KEY = "ticketId";
    private static final String ENTRY_STATE_KEY = "state";

    private final JwtParser jwtParser;

    public JwtEntryCodeExtractor(String secretKey) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.jwtParser = Jwts.parserBuilder()
            .setSigningKey(key)
            .build();
    }

    @Override
    public EntryCodePayload extract(String code) {
        Claims claims = getClaims(code);
        Long memberTicketId = claims.get(MEMBER_TICKET_ID_KEY, Long.class);
        EntryState entryState = EntryState.from(claims.get(ENTRY_STATE_KEY, Integer.class));

        return new EntryCodePayload(memberTicketId, entryState);
    }

    private Claims getClaims(String code) {
        try {
            return jwtParser.parseClaimsJws(code)
                .getBody();
        } catch (ExpiredJwtException e) {
            throw new BadRequestException(ErrorCode.EXPIRED_ENTRY_CODE);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadRequestException(ErrorCode.INVALID_ENTRY_CODE);
        }
    }
}
