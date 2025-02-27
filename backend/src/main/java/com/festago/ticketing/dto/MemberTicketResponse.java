package com.festago.ticketing.dto;

import com.festago.stage.domain.Stage;
import com.festago.stage.dto.StageResponse;
import com.festago.ticketing.domain.EntryState;
import com.festago.ticketing.domain.MemberTicket;
import java.time.LocalDateTime;

public record MemberTicketResponse(
    Long id,
    Integer number,
    LocalDateTime entryTime,
    EntryState state,
    LocalDateTime reservedAt,
    StageResponse stage,
    MemberTicketFestivalResponse festival) {

    private static final MemberTicketResponse EMPTY = new MemberTicketResponse(-1L, null, null, null, null, null, null);

    public static MemberTicketResponse from(MemberTicket memberTicket) {
        Stage stage = memberTicket.getStage();
        return new MemberTicketResponse(
            memberTicket.getId(),
            memberTicket.getNumber(),
            memberTicket.getEntryTime(),
            memberTicket.getEntryState(),
            memberTicket.getCreatedAt(),
            StageResponse.from(stage),
            MemberTicketFestivalResponse.from(stage.getFestival()));
    }

    public static MemberTicketResponse empty() {
        return EMPTY;
    }
}
