package com.festago.ticketing.application;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import com.festago.common.exception.BadRequestException;
import com.festago.common.exception.ErrorCode;
import com.festago.common.exception.NotFoundException;
import com.festago.member.repository.MemberRepository;
import com.festago.ticketing.domain.MemberTicket;
import com.festago.ticketing.dto.MemberTicketResponse;
import com.festago.ticketing.dto.MemberTicketsResponse;
import com.festago.ticketing.repository.MemberTicketRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberTicketService {

    private final MemberTicketRepository memberTicketRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MemberTicketResponse findById(Long memberId, Long memberTicketId) {
        validateMemberId(memberId);
        MemberTicket memberTicket = memberTicketRepository.findById(memberTicketId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_TICKET_NOT_FOUND));
        if (!memberTicket.isOwner(memberId)) {
            throw new BadRequestException(ErrorCode.NOT_MEMBER_TICKET_OWNER);
        }
        return MemberTicketResponse.from(memberTicket);
    }

    @Transactional(readOnly = true)
    public MemberTicketsResponse findAll(Long memberId, Pageable pageable) {
        validateMemberId(memberId);
        List<MemberTicket> memberTickets = memberTicketRepository.findAllByOwnerId(memberId, pageable);
        return memberTickets.stream()
            .collect(collectingAndThen(toList(), MemberTicketsResponse::from));
    }

    @Transactional(readOnly = true)
    public MemberTicketsResponse findCurrent(Long memberId, Pageable pageable) {
        validateMemberId(memberId);
        List<MemberTicket> memberTickets = memberTicketRepository.findAllByOwnerId(memberId, pageable);
        return MemberTicketsResponse.from(filterCurrentMemberTickets(memberTickets));
    }

    private List<MemberTicket> filterCurrentMemberTickets(List<MemberTicket> memberTickets) {
        LocalDateTime currentTime = LocalDateTime.now(clock);
        return memberTickets.stream()
            .filter(memberTicket -> memberTicket.isBeforeEntry(currentTime) || memberTicket.canEntry(currentTime))
            .sorted(comparing((MemberTicket memberTicket) -> memberTicket.isBeforeEntry(currentTime))
                .thenComparing(memberTicket -> calculateTimeGap(memberTicket, currentTime)))
            .toList();
    }

    private Duration calculateTimeGap(MemberTicket memberTicket, LocalDateTime time) {
        return Duration.between(memberTicket.getEntryTime(), time).abs();
    }

    private void validateMemberId(Long memberId) {
        memberRepository.findById(memberId)
            .orElseThrow(() -> new NotFoundException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
