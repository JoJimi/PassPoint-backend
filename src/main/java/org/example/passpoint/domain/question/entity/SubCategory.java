package org.example.passpoint.domain.question.entity;

/**
 * 질문 소분류
 * - 각 소분류는 자신이 속한 MainCategory를 가진다
 * - 잘못된 (Main, Sub) 조합을 구조적으로 방지
 */
public enum SubCategory {

    // CS
    NETWORK(MainCategory.CS),                           // 네트워크
    OPERATING_SYSTEM(MainCategory.CS),                  // 운영체제 (프로세스/스레드/메모리 포함)
    COMPUTER_ARCHITECTURE(MainCategory.CS),             // 컴퓨터 구조

    // LANGUAGE (Java)
    OOP(MainCategory.LANGUAGE),                         // 객체지향
    JVM(MainCategory.LANGUAGE),                         // JVM / 메모리 / GC
    COLLECTION(MainCategory.LANGUAGE),                  // 컬렉션
    CONCURRENCY(MainCategory.LANGUAGE),                 // 동시성 / 스레드
    EXCEPTION(MainCategory.LANGUAGE),                   // 예외 처리
    GENERIC(MainCategory.LANGUAGE),                     // 제네릭
    LAMBDA_STREAM(MainCategory.LANGUAGE),               // 람다 / 스트림
    ANNOTATION_REFLECTION(MainCategory.LANGUAGE),       // 어노테이션 / 리플렉션

    // SPRING
    SPRING_CORE(MainCategory.SPRING),                   // 컨테이너 / DI / IoC / 빈 생명주기
    SPRING_MVC(MainCategory.SPRING),                    // 웹 MVC
    SPRING_DATA_JPA(MainCategory.SPRING),               // 데이터 접근 / JPA
    SPRING_AOP(MainCategory.SPRING),                    // AOP
    SPRING_TRANSACTION(MainCategory.SPRING),            // 트랜잭션
    SPRING_SECURITY(MainCategory.SPRING),               // 스프링 시큐리티
    SPRING_BOOT(MainCategory.SPRING),                   // 스프링 부트 (자동설정 등)

    // DATA_STRUCTURE
    ARRAY_LIST(MainCategory.DATA_STRUCTURE),            // 배열 / 리스트 / 연결 리스트
    STACK_QUEUE(MainCategory.DATA_STRUCTURE),           // 스택 / 큐
    HASH(MainCategory.DATA_STRUCTURE),                  // 해시 / 해시테이블
    TREE(MainCategory.DATA_STRUCTURE),                  // 트리 / 힙

    // ALGORITHM
    SORTING(MainCategory.ALGORITHM),                    // 정렬
    SEARCHING(MainCategory.ALGORITHM),                  // 탐색
    DYNAMIC_PROGRAMMING(MainCategory.ALGORITHM),        // 동적 계획법
    GRAPH(MainCategory.ALGORITHM),                      // 그래프
    COMPLEXITY(MainCategory.ALGORITHM),                 // 시간/공간 복잡도
    GREEDY(MainCategory.ALGORITHM),                     // 그리디
    RECURSION(MainCategory.ALGORITHM),                  // 재귀 / 분할정복

    // DATABASE
    SQL(MainCategory.DATABASE),                         // SQL / 조인
    INDEX(MainCategory.DATABASE),                       // 인덱스
    TRANSACTION_DB(MainCategory.DATABASE),              // 트랜잭션 / 격리수준 / 락
    NORMALIZATION(MainCategory.DATABASE),               // 정규화
    NOSQL(MainCategory.DATABASE),                       // NoSQL

    // SECURITY
    AUTHENTICATION(MainCategory.SECURITY),              // 인증 / 인가 (JWT, OAuth, 세션, 토큰)
    ENCRYPTION(MainCategory.SECURITY),                  // 암호화 / HTTPS
    WEB_SECURITY(MainCategory.SECURITY),                // 웹 취약점 (XSS, CSRF 등)

    // INFRA / DevOps
    DOCKER(MainCategory.INFRA),                         // 컨테이너
    CI_CD(MainCategory.INFRA),                          // CI/CD / 배포 전략
    CLOUD(MainCategory.INFRA),                          // 클라우드 / 로드밸런싱
    MESSAGE_QUEUE(MainCategory.INFRA),                  // 메시지 큐 (Kafka 등)
    CACHE(MainCategory.INFRA),                          // 캐시 (Redis 등)
    MONITORING(MainCategory.INFRA),                     // 모니터링

    // SW_ARCHITECTURE
    DESIGN_PATTERN(MainCategory.SW_ARCHITECTURE),       // 디자인 패턴
    ARCHITECTURE_STYLE(MainCategory.SW_ARCHITECTURE),   // 아키텍처 스타일 (MSA, 모놀리식)
    OOP_DESIGN(MainCategory.SW_ARCHITECTURE),           // 객체지향 설계 (SOLID)
    CLEAN_CODE(MainCategory.SW_ARCHITECTURE),           // 클린 코드 / 리팩터링

    // WEB
    HTTP(MainCategory.WEB),                             // HTTP 프로토콜 / 메서드 / 상태코드
    REST(MainCategory.WEB),                             // REST API 설계
    COOKIE_SESSION(MainCategory.WEB),                   // 쿠키 / 세션
    WEBSOCKET(MainCategory.WEB);                        // 웹소켓 / 실시간 통신

    private final MainCategory mainCategory;

    SubCategory(MainCategory mainCategory) {
        this.mainCategory = mainCategory;
    }

    public MainCategory getMainCategory() {
        return mainCategory;
    }
}