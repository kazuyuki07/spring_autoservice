package com.pashinin.autoservice.dto;

import com.pashinin.autoservice.enums.Status;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class AutoServiceOrderDTO extends OrdersDTO{
    @NotBlank(message = "Имя сервиса не может быть пустым")
    private String name;

    @NotNull(message = "Цена не может быть пустой")
    @Min(value = 0, message = "Цена не может быть отрицательной")
    private Integer price;

    public AutoServiceOrderDTO(
            Long serviceId,
            String name,
            String clientName,
            LocalDate orderDate,
            Status status,
            Integer price) {
        super(serviceId, clientName, orderDate, status);
        this.name = name;
        this.price = price;
    }
}
