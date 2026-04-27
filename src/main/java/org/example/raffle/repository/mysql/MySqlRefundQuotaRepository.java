package org.example.raffle.repository.mysql;

import org.example.raffle.domain.RefundQuota;
import org.example.raffle.repository.RefundQuotaRepository;
import org.example.raffle.repository.mysql.mapper.RefundQuotaMapper;
import org.example.raffle.repository.mysql.po.RefundQuotaRow;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("!local")
public class MySqlRefundQuotaRepository implements RefundQuotaRepository {

    private final RefundQuotaMapper refundQuotaMapper;

    public MySqlRefundQuotaRepository(RefundQuotaMapper refundQuotaMapper) {
        this.refundQuotaMapper = refundQuotaMapper;
    }

    @Override
    public void initializeIfAbsent(Long userId, Long strategyId, int maxCount) {
        refundQuotaMapper.insertIgnore(userId, strategyId, maxCount);
    }

    @Override
    public Optional<RefundQuota> findByUserAndStrategy(Long userId, Long strategyId) {
        RefundQuotaRow row = refundQuotaMapper.findByUserAndStrategy(userId, strategyId);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new RefundQuota(
                row.getUserId(),
                row.getStrategyId(),
                row.getUsedCount() == null ? 0 : row.getUsedCount(),
                row.getMaxCount() == null ? 0 : row.getMaxCount(),
                row.getVersion() == null ? 0 : row.getVersion()
        ));
    }

    @Override
    public int incrementUsedWithVersion(Long userId, Long strategyId, int currentVersion) {
        return refundQuotaMapper.incrementUsedWithVersion(userId, strategyId, currentVersion);
    }
}
