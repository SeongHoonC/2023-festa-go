package com.festago.festago.data.dto

import com.festago.festago.domain.model.Ticket
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MemberTicketsResponse(
    @SerialName("tickets") val memberTickets: List<MemberTicketResponse>,
) {
    fun toDomain(): List<Ticket> = memberTickets.map { it.toDomain() }
}
