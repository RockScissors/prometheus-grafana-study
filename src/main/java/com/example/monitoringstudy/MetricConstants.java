package com.example.monitoringstudy;

public final class MetricConstants {

    // 인스턴스 생성 방지
    private MetricConstants() {
    }

    // public static final String 으로 메트릭 키값 고정

    // 메트릭 이름 (Base Name)
    public static final String TICKET_PAYMENT_REQUESTS = "ticket_payment_requests";
    public static final String TICKET_PURCHASE_REQUESTS = "ticket_purchase_requests";

    // 태그 키와 값 (Tags)
    public static final String TAG_STATUS = "status";
    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";

}
