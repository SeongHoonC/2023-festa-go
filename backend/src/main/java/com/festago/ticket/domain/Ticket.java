package com.festago.ticket.domain;

import com.festago.common.domain.BaseTimeEntity;
import com.festago.common.exception.BadRequestException;
import com.festago.common.exception.ErrorCode;
import com.festago.member.domain.Member;
import com.festago.school.domain.School;
import com.festago.stage.domain.Stage;
import com.festago.ticketing.domain.MemberTicket;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SortNatural;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ticket extends BaseTimeEntity {

    private static final int EARLY_ENTRY_LIMIT = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    private Stage stage;

    @NotNull
    private Long schoolId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TicketType ticketType;

    @OneToOne(mappedBy = "ticket", optional = false, fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private TicketAmount ticketAmount;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "ticket_id")
    @SortNatural
    private SortedSet<TicketEntryTime> ticketEntryTimes = new TreeSet<>();

    public Ticket(Stage stage, TicketType ticketType, School school) {
        this(null, stage, ticketType, school);
    }

    public Ticket(Stage stage, TicketType ticketType, Long schoolId) {
        this(null, stage, ticketType, schoolId);
    }

    public Ticket(Long id, Stage stage, TicketType ticketType, School school) {
        this(id, stage, ticketType, school.getId());
    }

    public Ticket(Long id, Stage stage, TicketType ticketType, Long schoolId) {
        validate(stage, ticketType);
        this.id = id;
        this.stage = stage;
        this.ticketType = ticketType;
        this.ticketAmount = new TicketAmount(this);
        this.schoolId = schoolId;
    }

    private void validate(Stage stage, TicketType ticketType) {
        checkNotNull(stage, ticketType);
    }

    private void checkNotNull(Stage stage, TicketType ticketType) {
        if (stage == null ||
            ticketType == null) {
            throw new IllegalArgumentException("Ticket 은 허용되지 않은 null 값으로 생성할 수 없습니다.");
        }
    }

    public void addTicketEntryTime(LocalDateTime currentTime, LocalDateTime entryTime, int amount) {
        validateEntryTime(currentTime, entryTime);
        TicketEntryTime ticketEntryTime = new TicketEntryTime(entryTime, amount);
        ticketAmount.addTotalAmount(amount);
        ticketEntryTimes.add(ticketEntryTime);
    }

    private void validateEntryTime(LocalDateTime currentTime, LocalDateTime entryTime) {
        LocalDateTime stageStartTime = stage.getStartTime();
        LocalDateTime ticketOpenTime = stage.getTicketOpenTime();
        if (currentTime.isEqual(ticketOpenTime) || currentTime.isAfter(ticketOpenTime)) {
            throw new BadRequestException(ErrorCode.INVALID_TICKET_CREATE_TIME);
        }
        if (entryTime.isBefore(ticketOpenTime) || entryTime.isEqual(ticketOpenTime)) {
            throw new BadRequestException(ErrorCode.EARLY_TICKET_ENTRY_THAN_OPEN);
        }
        if (entryTime.isAfter(stageStartTime) || entryTime.isEqual(stageStartTime)) {
            throw new BadRequestException(ErrorCode.LATE_TICKET_ENTRY_TIME);
        }
        if (entryTime.isBefore(stageStartTime.minusHours(EARLY_ENTRY_LIMIT))) {
            throw new BadRequestException(ErrorCode.EARLY_TICKET_ENTRY_TIME);
        }
    }

    public MemberTicket createMemberTicket(Member member, int reservationSequence, LocalDateTime currentTime) {
        if (stage.isStart(currentTime)) {
            throw new BadRequestException(ErrorCode.TICKET_CANNOT_RESERVE_STAGE_START);
        }
        LocalDateTime entryTime = calculateEntryTime(reservationSequence);
        return new MemberTicket(member, stage, reservationSequence, entryTime, ticketType);
    }

    private LocalDateTime calculateEntryTime(int reservationSequence) {
        int lastSequence = 0;
        for (TicketEntryTime ticketEntryTime : ticketEntryTimes) {
            lastSequence += ticketEntryTime.getAmount();
            if (reservationSequence <= lastSequence) {
                return ticketEntryTime.getEntryTime();
            }
        }
        throw new BadRequestException(ErrorCode.TICKET_SOLD_OUT);
    }

    public Long getId() {
        return id;
    }

    public Stage getStage() {
        return stage;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public TicketType getTicketType() {
        return ticketType;
    }

    public TicketAmount getTicketAmount() {
        return ticketAmount;
    }

    public Set<TicketEntryTime> getTicketEntryTimes() {
        return ticketEntryTimes;
    }
}
