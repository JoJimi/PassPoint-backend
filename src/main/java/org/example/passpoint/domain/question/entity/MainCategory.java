package org.example.passpoint.domain.question.entity;

/**
 * 질문 대분류
 * - 별도 테이블 없이 enum으로 관리 (고정된 카테고리)
 * - 카테고리별 질문 수 집계(/questions/categories/count)에 활용
 */
public enum MainCategory {
    CS,                     // 컴퓨터 기초 (네트워크, OS, DB, 자료구조 등)
    LANGUAGE,               // 프로그래밍 언어 (Java 중심)
    SPRING,                 // Spring 프레임워크
    DATA_STRUCTURE,         // 자료구조
    ALGORITHM,              // 알고리즘
    DATABASE,               // 데이터베이스
    SECURITY,               // 보안
    INFRA,                  // 인프라 / DevOps
    SW_ARCHITECTURE,        // 소프트웨어 아키텍처 / 설계
    WEB                     // 웹 / HTTP
}
