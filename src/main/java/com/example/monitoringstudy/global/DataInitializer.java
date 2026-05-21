package com.example.monitoringstudy.global;

import com.example.monitoringstudy.domain.Event;
import com.example.monitoringstudy.domain.Ticket;
import com.example.monitoringstudy.domain.Venue;
import com.example.monitoringstudy.repository.EventRepository;
import com.example.monitoringstudy.repository.TicketRepository;
import com.example.monitoringstudy.repository.VenueRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final VenueRepository venueRepository;
    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;

    @PostConstruct
    public void init() {
        if (venueRepository.count() > 0) return;

        for (int i = 1; i <= 10; i++) {
            Venue venue = new Venue();
            venueRepository.save(venue);

            for (int j = 1; j <= 10; j++) {
                Event event = new Event();
                eventRepository.save(event);

                Ticket ticket = new Ticket();
                ticketRepository.save(ticket);
            }
        }

        log.info("[DataInitializer] 테스트용 더미 데이터 삽입 완료");
    }
}
