package com.pashinin.autoservice.repositories;

import com.pashinin.autoservice.entities.AutoServices;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutoServicesRepository extends JpaRepository<AutoServices, Long> {
}
