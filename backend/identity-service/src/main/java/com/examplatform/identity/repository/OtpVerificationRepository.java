/*
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * National Assessment Grid (NAG) - Open Digital Public Infrastructure (DPI) Platform
 * Copyright (C) 2025 NAG Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.examplatform.identity.repository;

import com.examplatform.identity.domain.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, UUID> {

    Optional<OtpVerification> findTopByMobileHashAndVerifiedFalseOrderByCreatedAtDesc(String mobileHash);

    Optional<OtpVerification> findTopByUserIdAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(UUID userId, String otpType);

    Optional<OtpVerification> findTopByEmailHashAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(String emailHash, String otpType);

    Optional<OtpVerification> findTopByMobileHashAndOtpTypeAndVerifiedFalseOrderByCreatedAtDesc(String mobileHash, String otpType);

    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.userId = :userId AND o.channel = :channel AND o.createdAt >= :after")
    long countByUserIdAndChannelAndCreatedAtAfter(@Param("userId") UUID userId,
                                                 @Param("channel") String channel,
                                                 @Param("after") LocalDateTime after);

    @Query("SELECT COUNT(o) FROM OtpVerification o WHERE o.mobileHash = :mobileHash AND o.channel = :channel AND o.createdAt >= :after")
    long countByMobileHashAndChannelAndCreatedAtAfter(@Param("mobileHash") String mobileHash,
                                                     @Param("channel") String channel,
                                                     @Param("after") LocalDateTime after);

    @Query("SELECT MIN(o.createdAt) FROM OtpVerification o WHERE o.userId = :userId AND o.channel = :channel AND o.createdAt >= :after")
    Optional<LocalDateTime> findOldestSmsInWindow(@Param("userId") UUID userId,
                                                 @Param("channel") String channel,
                                                 @Param("after") LocalDateTime after);
}
