package org.example.raffle.repository;

import org.example.raffle.domain.RaffleRecord;

import java.util.List;

public interface RaffleRecordRepository {

    void save(RaffleRecord record);

    List<RaffleRecord> findAll();
}
