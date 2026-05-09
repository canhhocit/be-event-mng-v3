package com.sa.event_mng.modules.ordering.application.mapper;

import com.sa.event_mng.modules.ordering.application.dto.response.OrderResponse;
import com.sa.event_mng.modules.ordering.domain.model.Order;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {
    // Chuyển Order entity sang OrderResponse DTO trả về client.
    OrderResponse toOrderResponse(Order order);
}
