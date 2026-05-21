package com.example.monitoringstudy;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ApiController {

    private final ApiService apiService;

    // N+1
    @GetMapping("/events")
    public String getEvents() {
        // 공연(Event) 목록을 가져오는 쿼리가 1회 실행된 후,
        // 공연 객체 내부의 공연장(Venue) 정보를 참조하기 위한 하위 쿼리가 추가로 실행된다.
        // 데이터 건수가 늘어날수록 쿼리가 기하급수적으로 증가한다.
        return apiService.getEvents();
    }

    // 동시성
    @PostMapping("/tickets/purchase")
    public String purchaseTicket() {
        //인기 콘서트의 단 1개만 남은 잔여 좌석(Seat)에 몇백 명의 사용자가 동시에 예매 요청을 보낸다.
        // 서버 내부 로직에서   select  문으로 좌석 상태를 확인한 뒤
        // update 를 치는 과정 사이에 다중 스레드가 한꺼번에 진입한다.
        // 별도의 데이터베이스 락(비관적 락, 낙관적 락)이나 분산 락(Redis Redisson) 처리가 없기 때문에,
        // 동일한 좌석에 대한 여러 개의 성공 응답이 떨어지며 중복 예매 데이터가 생성된다.
        return apiService.purchaseTicket(1L);
    }

    // 커넥션 풀 고갈
    @PostMapping("/tickets/pay")
    public String payTicket() {
        // 결제 로직 메서드 상단에  @Transactional  어노테이션이 붙어 있어 진입과 동시에 DB 커넥션을 획득한다.
        // 메서드 내부에서 대기 시간이 길어져 커넥션을 반환하지 않고 계속 점유하게 되고,
        // 톰캣의 요청 스레드가 해당 API로 몰리면서 커넥션 풀의 개수가 고갈되며
        // 다른 일반 조회 API를 요청한 스레드들까지 DB 커넥션을 얻기 위해 무한 대기(Pending) 상태가 된다.
        apiService.payTicket();
        return "결제 대기 중...";
    }

    // 외부 API 타임아웃
    @PostMapping("/payments/external")
    public String requestExternalPayment() {
        // 사용자가 결제 버튼을 누르면 외부 카드사 시스템과 HTTP 통신을 하며 결제 승인을 받아야 하나,
        // 외부 서버의 장애로 응답이 오지 않는다. 스프링 내부의  RestTempate에 적절한 타임아웃 설정이
        // 되어 있지 않아 톰캣의 작업 스레드들이 외부 서버의 응답을 기다리며 블로킹(Blocking) 상태로 멈춰 서게 된다.
        // 결국 새로운 요청을 받을 수 있는 톰캣 스레드 풀이 고갈되어 서비스가 멈춘다.
        return apiService.requestExternalPayment();
    }

    //JVM 힙 메모리 누수
    @GetMapping("/admin/reservations")
    public String getAllReservations() {
        // 관리자가 대량의 예매 기록(Ticket)을 한 번에 조회하며,
        // JPA 영속성 컨텍스트에 한꺼번에 로드된 객체들이 개발자의 실수로 정적 컬렉션에 데이터가 누적되며 참조가 유지된다.
        // GC가 메모리를 해제하지 못해 Old Generation 영역이 지속해서 차오르고, OOM이 발생하며 서버가 다운된다.
        apiService.getAllReservations();
        return "관리자 예매 내역 조회 완료";
    }

    // 느린 쿼리 (미인덱싱)
    @GetMapping("tickets/search")
    public String searchTicketsByPhoneNumber() {
        // 비회원이 예매 내역을 확인하기 위해, 인덱스가 생성되어 있지 않은 phone_number 필드를 조건으로 검색한다.
        // DB 쿼리 수행 시 대량의 예매 데이터에 대해 전체 테이블을 처음부터 끝까지 스캔하는
        // Full Table Scan이 발생하여 디스크 I/O가 폭발하고,
        // 하나의 쿼리가 몇 초 이상 소요되며 DB 서버의 CPU 자원을 독점한다.
        return apiService.searchTicketsByPhoneNumber("010-1234-5678");
    }

    // 캐시 스탬피드
    @GetMapping("events/{id}/seats")
    public String getEventSeats(@PathVariable long id) {
        // DB 부하를 막기 위해 Redis 캐시에 좌석 정보를 저장하고 TTL을 5분으로 설정해 두었다.
        // 5분이 지나 Redis 캐시 키가 만료되어 사라지는 시점에 수천 건의 실시간 잔여 좌석에 대해
        // 동시 조회 요청이 몰려들게 되면,
        // 모든 요청이 캐시 레이어를 통과하지 못하고 전부 DB 서버로 쿼리를 날리게 된다.
        return apiService.getEventSeats(id);
    }

}
