package org.example.passpoint.domain.answer.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.passpoint.domain.answer.entity.Answer;
import org.example.passpoint.domain.answer.service.AnswerWriteService;
import org.example.passpoint.global.kafka.KafkaTopics;
import org.example.passpoint.global.kafka.event.AudioUploadedEvent;
import org.example.passpoint.global.s3.S3AudioStorageService;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.core.io.FileSystemResource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * audio.uploaded 소비 → S3 다운로드 → OpenAI Whisper STT → 변환 결과 저장 → feedback.requested 발행
 * - tx1(PENDING → TRANSCRIBING) → 트랜잭션 밖 S3 다운로드 + STT → tx2(answerText 저장 + feedback.requested 발행)
 * - audio.transcribed 토픽은 SttWorker가 bridge 없이 feedback.requested로 직접 이어준다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SttWorker {

    private final AnswerWriteService answerWriteService;
    private final S3AudioStorageService s3AudioStorageService;
    private final OpenAiAudioTranscriptionModel transcriptionModel;

    @KafkaListener(topics = KafkaTopics.AUDIO_UPLOADED)
    public void onAudioUploaded(AudioUploadedEvent event) {
        Long answerId = event.answerId();

        // tx1: 멱등 체크 + PENDING → TRANSCRIBING
        Answer answer = answerWriteService.markTranscribing(answerId);
        if (answer == null) {
            log.info("이미 처리된 음성 답변이라 건너뜀: answerId={}", answerId);
            return;
        }

        Path tempFile = null;
        try {
            // 트랜잭션 밖: S3에서 음성 파일 다운로드
            byte[] audioBytes = s3AudioStorageService.downloadAudio(answer.getAudioUrl());

            // 파일 확장자 추출 후 임시 파일에 저장 (Content-Type 정확한 전달을 위해)
            String audioKey = answer.getAudioUrl();
            String ext = audioKey.contains(".") ? audioKey.substring(audioKey.lastIndexOf('.')) : ".m4a";
            tempFile = Files.createTempFile("stt-", ext);
            Files.write(tempFile, audioBytes);

            // 트랜잭션 밖: OpenAI Whisper STT
            var audioResource = new FileSystemResource(tempFile);
            var options = OpenAiAudioTranscriptionOptions.builder()
                    .model("whisper-1")
                    .build();
            var response = transcriptionModel.call(new AudioTranscriptionPrompt(audioResource, options));
            String transcript = response.getResult().getOutput();

            log.info("STT 완료: answerId={}, transcript 길이={}", answerId, transcript.length());

            // tx2: 변환 결과 저장 + feedback.requested 발행(AFTER_COMMIT)
            answerWriteService.completeTranscription(answerId, transcript);

        } catch (Exception e) {
            log.error("STT 처리 실패: answerId={}", answerId, e);
            answerWriteService.markFailed(answerId);
        } finally {
            if (tempFile != null) {
                try { Files.deleteIfExists(tempFile); } catch (IOException ignored) {}
            }
        }
    }
}
