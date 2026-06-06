package org.example.passpoint.domain.answer.entity;

/**
 * 답변 입력 방식
 * - TEXT: 텍스트로 직접 작성 → answerText 사용
 * - VOICE: 음성 녹음 → audioUrl/audioDuration 사용, STT 변환 후 answerText 채움
 */
public enum AnswerType {
    TEXT, VOICE
}
