package com.festago.festago.presentation.ui.ticketreserve

import app.cash.turbine.test
import com.festago.festago.analytics.AnalyticsHelper
import com.festago.festago.model.Reservation
import com.festago.festago.model.ReservationStage
import com.festago.festago.model.ReservationTicket
import com.festago.festago.model.ReservationTickets
import com.festago.festago.model.ReservedTicket
import com.festago.festago.model.TicketType
import com.festago.festago.presentation.rule.MainDispatcherRule
import com.festago.festago.repository.AuthRepository
import com.festago.festago.repository.FestivalRepository
import com.festago.festago.repository.ReservationTicketRepository
import com.festago.festago.repository.TicketRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TicketReserveViewModelTest {

    private lateinit var vm: TicketReserveViewModel
    private lateinit var reservationTicketRepository: ReservationTicketRepository
    private lateinit var festivalRepository: FestivalRepository
    private lateinit var ticketRepository: TicketRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var analyticsHelper: AnalyticsHelper

    private val fakeReservationTickets = ReservationTickets(
        listOf(
            ReservationTicket(1, TicketType.STUDENT, 219, 500),
            ReservationTicket(1, TicketType.VISITOR, 212, 300),
        ),
    )
    private val fakeReservationStage = ReservationStage(
        id = 1,
        lineUp = "르세라핌, 아이브, 뉴진스",
        reservationTickets = fakeReservationTickets,
        startTime = LocalDateTime.now(),
        ticketOpenTime = LocalDateTime.now(),
    )
    private val fakeReservationStages = List(5) { fakeReservationStage }
    private val fakeReservation = Reservation(
        id = 1,
        name = "테코대학교",
        reservationStages = fakeReservationStages,
        startDate = LocalDate.now(),
        endDate = LocalDate.now(),
        thumbnail = "https://search2.kakaocdn.net/argon/656x0_80_wr/8vLywd3V06c",
    )

    private val fakeReservedTicket = ReservedTicket(
        id = 1,
        entryTime = LocalDateTime.now(),
        number = 1,
    )

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        reservationTicketRepository = mockk()
        festivalRepository = mockk()
        ticketRepository = mockk()
        authRepository = mockk()
        analyticsHelper = mockk(relaxed = true)
        vm = TicketReserveViewModel(
            reservationTicketRepository,
            festivalRepository,
            ticketRepository,
            authRepository,
            analyticsHelper,
        )
    }

    private fun `예약 정보 요청 결과가 다음과 같을 때`(result: Result<Reservation>) {
        coEvery { festivalRepository.loadFestivalDetail(any()) } returns result
    }

    private fun `인증 여부가 다음과 같을 때`(isSigned: Boolean) {
        coEvery { authRepository.isSigned } answers { isSigned }
    }

    private fun `특정 공연의 티켓 타입 요청 결과가 다음과 같을 때`(result: Result<ReservationTickets>) {
        coEvery { reservationTicketRepository.loadTicketTypes(any()) } returns result
    }

    private fun `티켓 예약 요청 결과가 다음과 같을 때`(result: Result<ReservedTicket>) {
        coEvery { ticketRepository.reserveTicket(any()) } returns result
    }

    @Test
    fun `예약 정보를 불러오면 성공 이벤트가 발생하고 리스트를 반환한다`() {
        // given
        `예약 정보 요청 결과가 다음과 같을 때`(Result.success(fakeReservation))
        `인증 여부가 다음과 같을 때`(true)

        // when
        vm.loadReservation()

        // then
        assertThat(vm.uiState.value).isInstanceOf(TicketReserveUiState.Success::class.java)

        // and
        val festival = (vm.uiState.value as TicketReserveUiState.Success).festival
        val expected = ReservationFestivalUiState(
            id = festival.id,
            name = festival.name,
            thumbnail = festival.thumbnail,
            endDate = festival.endDate,
            startDate = festival.startDate,
        )
        assertThat(festival).isEqualTo(expected)
    }

    @Test
    fun `예약 정보를 불러오는 것을 실패하면 에러 이벤트가 발생한다`() {
        // given
        `예약 정보 요청 결과가 다음과 같을 때`(Result.failure(Exception()))

        // when
        vm.loadReservation(0)

        // then
        assertThat(vm.uiState.value).isEqualTo(TicketReserveUiState.Error)
    }

    @Test
    fun `예약 정보를 불러오는 중이면 로딩 이벤트가 발생한다`() {
        // given
        coEvery {
            festivalRepository.loadFestivalDetail(0)
        } coAnswers {
            delay(1000)
            Result.success(fakeReservation)
        }

        // when
        vm.loadReservation()

        // then
        assertThat(vm.uiState.value).isEqualTo(TicketReserveUiState.Loading)
    }

    @Test
    fun `특정 공연의 티켓 타입을 보여주는 이벤트가 발생하면 해당 공연의 티켓 타입을 보여준다`() = runTest {
        // given
        `특정 공연의 티켓 타입 요청 결과가 다음과 같을 때`(Result.success(fakeReservationTickets))
        `인증 여부가 다음과 같을 때`(true)

        vm.event.test {
            // when
            vm.showTicketTypes(1, LocalDateTime.MIN)

            // then
            val softly = SoftAssertions().apply {
                val event = awaitItem()
                assertThat(event).isExactlyInstanceOf(TicketReserveEvent.ShowTicketTypes::class.java)

                // and
                val actual = (event as? TicketReserveEvent.ShowTicketTypes)?.tickets
                assertThat(actual).isEqualTo(fakeReservationTickets.sortedByTicketTypes())
            }
            softly.assertAll()
        }
    }

    @Test
    fun `특정 공연의 티켓 타입을 보여주는 것을 실패하면 에러 이벤트가 발생한다`() {
        // given
        `특정 공연의 티켓 타입 요청 결과가 다음과 같을 때`(Result.failure(Exception()))
        `인증 여부가 다음과 같을 때`(true)

        // when
        vm.showTicketTypes(1, LocalDateTime.MIN)

        // then
        assertThat(vm.uiState.value).isEqualTo(TicketReserveUiState.Error)
    }

    @Test
    fun `티켓 유형을 선택하고 예약하면 예약 성공 이벤트가 발생한다`() = runTest {
        // given
        coEvery {
            ticketRepository.reserveTicket(any())
        } answers {
            Result.success(fakeReservedTicket)
        }

        vm.event.test {
            // when
            vm.reserveTicket(0)

            // then
            assertThat(awaitItem()).isExactlyInstanceOf(TicketReserveEvent.ReserveTicketSuccess::class.java)
        }
    }

    @Test
    fun `티켓 유형을 선택하고 예약하는 것을 실패하면 예약 실패 이벤트가 발생한다`() = runTest {
        // given
        `티켓 예약 요청 결과가 다음과 같을 때`(Result.failure(Exception()))

        vm.event.test {
            // when
            vm.reserveTicket(0)

            // then
            assertThat(awaitItem()).isExactlyInstanceOf(TicketReserveEvent.ReserveTicketFailed::class.java)
        }
    }
}
