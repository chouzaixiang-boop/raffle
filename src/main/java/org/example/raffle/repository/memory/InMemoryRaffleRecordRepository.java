package org.example.raffle.repository.memory;

import org.example.raffle.domain.RaffleRecord;
import org.example.raffle.repository.RaffleRecordRepository;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

import java.util.ArrayList;
import java.util.List;

@Repository
@Profile("local")
public class InMemoryRaffleRecordRepository implements RaffleRecordRepository {

    private final List<RaffleRecord> records = new ArrayList<>();

    @Override
    public synchronized void save(RaffleRecord record) {
        records.add(record);
    }

    @Override
    public synchronized List<RaffleRecord> findAll() {
        return List.copyOf(records);
    }
}
