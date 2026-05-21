package com.example.monitoringstudy;

import com.example.monitoringstudy.config.CacheConfig;
import com.example.monitoringstudy.domain.Event;
import com.example.monitoringstudy.domain.Ticket;
import com.example.monitoringstudy.repository.EventRepository;
import com.example.monitoringstudy.repository.TicketRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ApiService {

    private final MeterRegistry meterRegistry; // 커스텀 메트릭용

    private final EventRepository eventRepository;
    private final TicketRepository ticketRepository;


    // 메모리 누수 발생용 static 리스트 (GC 수거 X)
    private static final List<String> memoryLeakList = new ArrayList<>();


    // N+1
    public String getEvents() {
        List<Event> events = eventRepository.findAll();

        for (Event event : events) {
            String venueName = event.getVenue().getName();
        }

        return "N+1 쿼리 폭발 완료";
    }

    // 동시성
    @Transactional
    public String purchaseTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow();

        if (ticket.isPaid()) {
            throw new RuntimeException("이미 결제된 티켓입니다.");
        }

        // 결제-업데이트 로직 사이 여러 스레드가 들어올 수 있도록 의도적으로 시간 지연
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ticket.pay();
        meterRegistry.counter("ticket.purchase.success").increment();

        return "예매 완료!";
    }


    // 커넥션 풀 고갈
    // 스프링 기본 설정 - 메서드에 @Transactional이 붙은 메서드에 진입하는 순간,
    // DB 커넥션을 하나 가져오고 메서드가 끝날 때까지 놓지 않음
    // 실제 DB에 접근하지 않아도 DB 커넥션 풀을 물고 있는 상태
    @Transactional
    public void payTicket() {
        try {
            // @Transactional 안에서 DB 커넥션 1개를 5초간 독점
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 외부 API 타임아웃
    public String requestExternalPayment() {
        try {
            // 외부 망 통신이 느린 상황 가정
            Thread.sleep(3000);
            // 성공 시 커스텀 메트릭 success 1 증가
            meterRegistry.counter("custom.pg.success").increment();
            return "Payment Success";
        } catch (InterruptedException e) {
            // 실패 시 커스텀 메트릭 error 1 증가
            meterRegistry.counter("custom.pg.error").increment();
            throw new RuntimeException("결제 서버 응답 없음");
        }
    }

    // JVM 힙 메모리 누수 (OOM)
    public void getAllReservations() {
        // API가 호출될 때마다 메모리가 반환되지 않고 힙 영역 끊임없이 우상향
        for (int i = 0; i < 100000; i++) {
            memoryLeakList.add("Dummy Reservation Data " + System.currentTimeMillis());
        }
    }

    // 느린 쿼리
    public String searchTicketsByPhoneNumber(String phoneNumber) {
        List<Ticket> tickets = ticketRepository.findByPhoneNumber(phoneNumber);

        try {
            // 대량의 데이터를 풀 스캔할 때 발생하는 하드웨어 I/O 대기 시간과
            // DB 서버 CPU 병목 시간을 2초로 가정
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return "미인덱싱 컬럼 조건 검색 완료 (Full Table Scan 발생)";
    }

    // 캐시 스탬피드
    @Cacheable(cacheNames = CacheConfig.EVENT_SEATS_CACHE, key = "#eventId")
    public String getEventSeats(Long eventId) {

        // 실제 캐시 미스는 주로 debug,
        // 콘솔에서 상황을 확인하기 위해 warn 설정
        log.warn("[Cache Miss] Event Id: {}", eventId);

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return eventId + "번 공연 잔여 좌석 조회 완료";
    }

}
