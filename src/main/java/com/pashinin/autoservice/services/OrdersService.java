package com.pashinin.autoservice.services;

import com.pashinin.autoservice.dto.OrdersDTO;
import com.pashinin.autoservice.entities.Orders;
import com.pashinin.autoservice.enums.Status;
import com.pashinin.autoservice.exceptions.NotFoundException;
import com.pashinin.autoservice.repositories.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrdersService {
    @Value("${spring.schedule.clean-service-history.deadline-by-year}")
    private Integer deadLineByYear;

    private final OrdersRepository ordersRepository;

    public Orders createOrder(OrdersDTO ordersDTO) {
        Orders order = Orders.builder()
                .serviceId(ordersDTO.getServiceId())
                .clientName(ordersDTO.getClientName())
                .orderDate(ordersDTO.getOrderDate())
                .status(ordersDTO.getStatus())
                .build();
        return ordersRepository.save(order);
    }

    public Orders updateOrderStatusById(Long id, String status) {
        Orders order = ordersRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Заказ с id " + id + " не найден"));

        order.setStatus(Status.valueOf(status));

        return ordersRepository.save(order);
    }

    public void deleteOrderById(Long id) {
        ordersRepository.deleteById(id);
    }

    @Scheduled(cron = "${spring.schedule.clean-service-history.cron}")
    public void cleanOrderByDate() {
        log.info("Cleaning order history by deadline: {}", deadLineByYear);
        List<Long> idListByDate = ordersRepository.getIdListByOrderDate(deadLineByYear, LocalDateTime.now().getYear());

        if (idListByDate.isEmpty()) {
            return;
        }

        ordersRepository.deleteAllById(idListByDate);
        log.info("Deleted {} orders older than {} years", idListByDate.size(), deadLineByYear);
    }
}
