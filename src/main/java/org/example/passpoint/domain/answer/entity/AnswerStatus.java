package org.example.passpoint.domain.answer.entity;

/**
 * 답변 처리 상태 (비동기 파이프라인)
 * - PENDING: 제출 접수, 처리 대기
 * - TRANSCRIBING: STT 변환 중 (음성 답변만)
 * - ANALYZING: AI 피드백 분석 중
 * - DONE: 처리 완료 (피드백 조회 가능)
 * - FAILED: 처리 실패
 */
public enum AnswerStatus {
    PENDING, TRANSCRIBING, ANALYZING, DONE, FAILED
}