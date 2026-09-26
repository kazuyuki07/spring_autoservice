package com.pashinin.autoservice.controller;

import com.pashinin.autoservice.dto.AutoServiceOrderDTO;
import com.pashinin.autoservice.dto.AutoServicesDTO;
import com.pashinin.autoservice.dto.OrdersDTO;
import com.pashinin.autoservice.entities.AutoServices;
import com.pashinin.autoservice.entities.Orders;
import com.pashinin.autoservice.services.AutoserviceService;
import com.pashinin.autoservice.services.OrdersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/autoservice")
public class AutoserviceController {
    private final AutoserviceService autoserviceService;
    private final OrdersService ordersService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AutoServiceOrderDTO createAutoService(@Valid @RequestBody AutoServiceOrderDTO inputOrder) {
        AutoServices autoService = autoserviceService.createAutoService(
                new AutoServicesDTO(
                        inputOrder.getName(),
                        inputOrder.getPrice()
                )
        );
        Orders order = ordersService.createOrder(
                new OrdersDTO(
                        autoService.getId(),
                        inputOrder.getClientName(),
                        inputOrder.getOrderDate(),
                        inputOrder.getStatus()
                )
        );

        return new AutoServiceOrderDTO(
                order.getServiceId(),
                autoService.getName(),
                order.getClientName(),
                order.getOrderDate(),
                order.getStatus(),
                autoService.getPrice()
        );
    }

    @PatchMapping("/status/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    public AutoServiceOrderDTO updateOrderStatusById(@PathVariable Long orderId, @RequestParam String status) {
        Orders changedOrder = ordersService.updateOrderStatusById(orderId, status);
        AutoServices autoService = autoserviceService.getAutoServiceById(changedOrder.getServiceId());

        return new AutoServiceOrderDTO(
                changedOrder.getServiceId(),
                autoService.getName(),
                changedOrder.getClientName(),
                changedOrder.getOrderDate(),
                changedOrder.getStatus(),
                autoService.getPrice()
        );
    }
}
