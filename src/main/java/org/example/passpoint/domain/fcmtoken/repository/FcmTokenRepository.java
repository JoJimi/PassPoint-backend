package org.example.passpoint.domain.fcmtoken.repository;

import org.example.passpoint.domain.fcmtoken.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    Optional<FcmToken> findByIdAndUserId(Long id, Long userId);

    List<FcmToken> findAllByUserId(Long userId);

    @Modifying
    @Query("UPDATE FcmToken f SET f.updatedAt = CURRENT_TIMESTAMP WHERE f.id = :id")
    void touchUpdatedAt(@Param("id") Long id);
}
