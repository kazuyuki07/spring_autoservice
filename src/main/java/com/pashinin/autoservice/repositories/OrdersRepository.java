package com.pashinin.autoservice.repositories;

import com.pashinin.autoservice.entities.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
    @Query("""
        SELECT o.id
        FROM Orders o
        WHERE (YEAR(o.orderDate) + :deadlineByYear) <= :currentYear
    """)
    List<Long> getIdListByOrderDate(@Param("deadlineByYear") Integer deadlineByYear, @Param("currentYear") Integer currentYear);
}
