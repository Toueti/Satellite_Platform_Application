package com.enit.satellite_platform.modules.user_management.management_cvore_service.repositories;

import com.enit.satellite_platform.modules.user_management.management_cvore_service.entities.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {

    Optional<RefreshToken> findByToken(String token);

    @Query("{ 'userId': ?0, 'expiryDate': { $gt: ?1 }, 'isRevoked': false }")
    List<RefreshToken> findValidTokensByUserId(String userId, Date currentDate);

    int countByUserIdAndIsRevokedFalseAndExpiryDateAfter(String userId, Date currentDate);

    @Query("{ 'device._id': ?0 }")
    List<RefreshToken> findByDeviceId(String deviceId);

    @Query("{ 'expiryDate': { $lt: ?0 } }")
    List<RefreshToken> findAllExpiredTokens(Date currentDate);

    void deleteByUserId(String userId);
}
