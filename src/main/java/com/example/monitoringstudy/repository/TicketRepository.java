package com.example.monitoringstudy.repository;

import com.example.monitoringstudy.domain.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findByPhoneNumber(String phoneNumber);
}
