package com.pashinin.autoservice.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pashinin.autoservice.enums.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class OrdersDTO {
    private Long serviceId;

    @NotBlank(message = "Имя клиента не может быть пустым")
    private String clientName;

    @NotNull(message = "Дата не может быть пустой")
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate orderDate;

    @NotNull(message = "Статус не может быть пустым")
    private Status status;
}
