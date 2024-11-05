package com.br.calculator.services;

import com.br.calculator.entities.Record;
import com.br.calculator.entities.User;
import com.br.calculator.exceptions.UserException;
import com.br.calculator.repositories.RecordRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

@Service
public class RecordService {

    private final RecordRepository recordRepository;

    public RecordService(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public Optional<Record> findById(Long id, User user) {
        Optional<Record> record = recordRepository.findByIdAndActive(id, Boolean.TRUE);
        if (record.isPresent() && !Objects.equals(record.get().getUser().getId(), user.getId())) {
            throw new UserException("Action not allowed");
        }
        return record;
    }

    public void deleteRecord(Record record) {
        record.setActive(false);
        recordRepository.save(record);
    }

}
