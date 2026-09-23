package com.sa.event_mng.model.enums;

public enum OrderStatus {
    PENDING, //chờ thanh toán hoặc xác nhận đơn hàng
    CONFIRMED, //đơn hàng dc xác nhận
    CANCELLED, //đơn bị hủy
    COMPLETED //đơn hàng hoàn thành
}
