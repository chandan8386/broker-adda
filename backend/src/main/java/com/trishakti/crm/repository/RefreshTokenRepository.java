package com.trishakti.crm.repository;

import com.trishakti.crm.domain.RefreshToken;
import com.trishakti.crm.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHashAndRevokedFalse(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revoked = true where t.user = :user and t.revoked = false")
    void revokeAllForUser(@Param("user") User user);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
