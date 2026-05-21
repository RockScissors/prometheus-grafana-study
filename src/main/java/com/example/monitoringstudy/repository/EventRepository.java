package com.example.monitoringstudy.repository;

import com.example.monitoringstudy.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {

}
