package com.sa.event_mng.modules.ordering.application.mapper;

import com.sa.event_mng.modules.ordering.application.dto.response.CartItemResponse;
import com.sa.event_mng.modules.ordering.application.dto.response.CartResponse;
import com.sa.event_mng.modules.ordering.domain.model.Cart;
import com.sa.event_mng.modules.ordering.domain.model.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CartMapper {

    // Chuyển Cart entity sang CartResponse DTO để trả về API.
    @Mapping(target = "totalAmount", expression = "java(calculateTotal(cart.getItems()))")
    @Mapping(target = "items", source = "items", qualifiedByName = "mapCartItems")
    CartResponse toCartResponse(Cart cart);

    @Named("mapCartItems")
    default List<CartItemResponse> mapCartItems(List<CartItem> items) {
        // Chuyển danh sách CartItem sang danh sách CartItemResponse.
        if (items == null) {
            return new ArrayList<>();
        }
        return items.stream()
                .map(this::toCartItemResponse)
                .collect(Collectors.toList());
    }

    @Mapping(target = "ticketTypeId", source = "ticketType.id")
    @Mapping(target = "ticketTypeName", source = "ticketType.name")
    @Mapping(target = "eventName", source = "ticketType.event.name")
    @Mapping(target = "eventId", source = "ticketType.event.id")
    @Mapping(target = "eventImage", expression = "java(cartItem.getTicketType().getEvent().getImages() != null && !cartItem.getTicketType().getEvent().getImages().isEmpty() ? cartItem.getTicketType().getEvent().getImages().get(0).getImageUrl() : null)")
    @Mapping(target = "saleEndDate", source = "ticketType.event.saleEndDate")
    CartItemResponse toCartItemResponse(CartItem cartItem);

    default BigDecimal calculateTotal(List<CartItem> items) {
        // Tính tổng tiền của tất cả item trong giỏ hàng.
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(CartItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
