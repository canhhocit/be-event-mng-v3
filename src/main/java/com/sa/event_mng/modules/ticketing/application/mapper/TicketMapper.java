package com.sa.event_mng.modules.ticketing.application.mapper;

import com.sa.event_mng.modules.ticketing.application.dto.response.TicketResponse;
import com.sa.event_mng.modules.ticketing.domain.model.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketMapper {
    // Chuyển Ticket entity sang TicketResponse DTO.
    @Mapping(target = "eventName", source = "ticketType.event.name")
    @Mapping(target = "ticketTypeName", source = "ticketType.name")
    TicketResponse toTicketResponse(Ticket ticket);
}
