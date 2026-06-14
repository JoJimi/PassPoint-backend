package org.example.passpoint.domain.studylog.repository;

import org.example.passpoint.domain.studylog.entity.StudyLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface StudyLogRepository extends JpaRepository<StudyLog, Long> {

    Optional<StudyLog> findByUserIdAndStudyDate(Long userId, LocalDate studyDate);
}
