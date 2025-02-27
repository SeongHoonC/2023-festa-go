package com.festago.domain;

import static com.festago.common.exception.ErrorCode.EARLY_TICKET_ENTRY_THAN_OPEN;
import static com.festago.common.exception.ErrorCode.EARLY_TICKET_ENTRY_TIME;
import static com.festago.common.exception.ErrorCode.INVALID_TICKET_CREATE_TIME;
import static com.festago.common.exception.ErrorCode.LATE_TICKET_ENTRY_TIME;
import static com.festago.common.exception.ErrorCode.TICKET_CANNOT_RESERVE_STAGE_START;
import static com.festago.common.exception.ErrorCode.TICKET_SOLD_OUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.festago.common.exception.BadRequestException;
import com.festago.festival.domain.Festival;
import com.festago.member.domain.Member;
import com.festago.stage.domain.Stage;
import com.festago.support.FestivalFixture;
import com.festago.support.MemberFixture;
import com.festago.support.StageFixture;
import com.festago.support.TicketFixture;
import com.festago.ticket.domain.Ticket;
import com.festago.ticketing.domain.MemberTicket;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator.ReplaceUnderscores;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayNameGeneration(ReplaceUnderscores.class)
@SuppressWarnings("NonAsciiCharacters")
class TicketTest {

    @Nested
    class 입장시간_추가_검증 {

        @ParameterizedTest
        @ValueSource(longs = {0, 1})
        void 입장시간이_티켓오픈시간_이전이면_예외(long minute) {
            // given
            LocalDateTime now = LocalDateTime.now();
            Stage stage = StageFixture.stage()
                .startTime(now.plusDays(1))
                .ticketOpenTime(now)
                .build();
            Ticket ticket = TicketFixture.ticket()
                .stage(stage)
                .build();

            // when & then
            assertThatThrownBy(
                () -> ticket.addTicketEntryTime(now.minusMinutes(10), now.minusMinutes(minute), 100))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(EARLY_TICKET_ENTRY_THAN_OPEN.getMessage());
        }

        @ParameterizedTest
        @ValueSource(longs = {0, 1})
        void 입장_시간이_축제_시작_시간보다_같거나_이후면_예외(long minute) {
            // given
            Ticket ticket = TicketFixture.ticket()
                .build();

            Stage stage = ticket.getStage();
            LocalDateTime stageStartTime = stage.getStartTime();
            LocalDateTime entryTime = stageStartTime.plusMinutes(minute);
            LocalDateTime ticketOpenTime = stage.getTicketOpenTime();

            // when & then
            assertThatThrownBy(() -> ticket.addTicketEntryTime(ticketOpenTime.minusMinutes(10), entryTime, 100))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(LATE_TICKET_ENTRY_TIME.getMessage());
        }

        @Test
        void 입장_시간이_공연_시작_12시간_이전이면_예외() {
            // given
            Ticket ticket = TicketFixture.ticket()
                .build();

            Stage stage = ticket.getStage();
            LocalDateTime stageStartTime = stage.getStartTime();
            LocalDateTime entryTime = stageStartTime.minusHours(12).minusSeconds(1);
            LocalDateTime ticketOpenTime = stage.getTicketOpenTime();

            // when & then
            assertThatThrownBy(() -> ticket.addTicketEntryTime(ticketOpenTime.minusMinutes(10), entryTime, 100))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(EARLY_TICKET_ENTRY_TIME.getMessage());
        }

        @Test
        void 티켓_오픈_이후_티켓생성시_예외() {
            // given
            Stage stage = StageFixture.stage()
                .ticketOpenTime(LocalDateTime.now().minusHours(1))
                .build();
            Ticket ticket = TicketFixture.ticket()
                .build();

            LocalDateTime startTime = stage.getStartTime();

            // when & then
            assertThatThrownBy(() -> ticket.addTicketEntryTime(LocalDateTime.now(), startTime.minusHours(3), 100))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(INVALID_TICKET_CREATE_TIME.getMessage());
        }

        @Test
        void 입장시간을_추가한다() {
            // given
            Ticket ticket = TicketFixture
                .ticket()
                .build();

            Stage stage = ticket.getStage();
            LocalDateTime startTime = stage.getStartTime();
            LocalDateTime ticketOpenTime = stage.getTicketOpenTime();

            // when
            ticket.addTicketEntryTime(ticketOpenTime.minusMinutes(10), startTime.minusHours(3), 100);
            ticket.addTicketEntryTime(ticketOpenTime.minusMinutes(10), startTime.minusHours(2), 200);

            // then
            assertSoftly(softly -> {
                softly.assertThat(ticket.getTicketAmount().getTotalAmount()).isEqualTo(300);
                softly.assertThat(ticket.getTicketEntryTimes()).hasSize(2);
            });
        }
    }

    @Nested
    class 예매_티켓_생성 {

        @Test
        void 최대_수량보다_많으면_예외() {
            // given
            LocalDateTime stageStartTime = LocalDateTime.parse("2022-08-12T18:00:00");
            LocalDateTime now = stageStartTime.minusHours(6);
            Festival festival = FestivalFixture.festival()
                .startDate(stageStartTime.toLocalDate())
                .endDate(stageStartTime.toLocalDate())
                .build();
            Stage stage = StageFixture.stage()
                .startTime(stageStartTime)
                .ticketOpenTime(stageStartTime.minusDays(1))
                .festival(festival)
                .build();
            Ticket ticket = TicketFixture.ticket()
                .stage(stage)
                .build();
            Member member = MemberFixture.member()
                .id(1L)
                .build();

            // when & then
            assertThatThrownBy(() -> ticket.createMemberTicket(member, 101, now))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(TICKET_SOLD_OUT.getMessage());
        }

        @Test
        void 공연의_시간이_지나고_예매하면_예외() {
            LocalDateTime stageStartTime = LocalDateTime.parse("2022-08-12T18:00:00");
            LocalDateTime now = stageStartTime.plusHours(1);
            Festival festival = FestivalFixture.festival()
                .startDate(stageStartTime.toLocalDate())
                .endDate(stageStartTime.toLocalDate())
                .build();
            Stage stage = StageFixture.stage()
                .startTime(stageStartTime)
                .ticketOpenTime(stageStartTime.minusDays(1))
                .festival(festival)
                .build();
            Ticket ticket = TicketFixture.ticket()
                .stage(stage)
                .build();
            Member member = MemberFixture.member()
                .id(1L)
                .build();

            ticket.addTicketEntryTime(LocalDateTime.MIN, stageStartTime.minusHours(1), 100);

            // when & then
            assertThatThrownBy(() -> ticket.createMemberTicket(member, 1, now))
                .isInstanceOf(BadRequestException.class)
                .hasMessage(TICKET_CANNOT_RESERVE_STAGE_START.getMessage());
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 100})
        void 성공(int reservationSequence) {
            // given
            LocalDateTime stageStartTime = LocalDateTime.parse("2022-08-12T18:00:00");
            LocalDateTime now = stageStartTime.minusHours(6);
            Festival festival = FestivalFixture.festival()
                .startDate(stageStartTime.toLocalDate())
                .endDate(stageStartTime.toLocalDate())
                .build();
            Stage stage = StageFixture.stage()
                .startTime(stageStartTime)
                .ticketOpenTime(stageStartTime.minusDays(1))
                .festival(festival)
                .build();
            Ticket ticket = TicketFixture.ticket()
                .stage(stage)
                .build();
            Member member = MemberFixture.member()
                .id(1L)
                .build();

            ticket.addTicketEntryTime(LocalDateTime.MIN, stageStartTime.minusHours(1), 50);
            ticket.addTicketEntryTime(LocalDateTime.MIN, stageStartTime.minusHours(2), 30);
            ticket.addTicketEntryTime(LocalDateTime.MIN, stageStartTime.minusHours(3), 20);

            // when
            MemberTicket memberTicket = ticket.createMemberTicket(member, reservationSequence, now);

            // then
            assertThat(memberTicket.getOwner()).isEqualTo(member);
        }
    }
}
