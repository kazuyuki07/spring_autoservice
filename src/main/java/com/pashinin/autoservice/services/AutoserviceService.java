package com.pashinin.autoservice.services;


import com.pashinin.autoservice.dto.AutoServicesDTO;
import com.pashinin.autoservice.entities.AutoServices;
import com.pashinin.autoservice.exceptions.NotFoundException;
import com.pashinin.autoservice.repositories.AutoServicesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutoserviceService {
    private final AutoServicesRepository autoServicesRepository;

    public AutoServices createAutoService(AutoServicesDTO autoServicesDTO) {
        AutoServices autoService = AutoServices.builder()
                .name(autoServicesDTO.getName())
                .price(autoServicesDTO.getPrice())
                .build();

        return autoServicesRepository.save(autoService);
    }

    public AutoServices getAutoServiceById(Long id) {
        return autoServicesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Сервис с id " + id + " не найден"));
    }

    public void deleteAutoServiceById(Long id) {
        autoServicesRepository.deleteById(id);
    }
}
