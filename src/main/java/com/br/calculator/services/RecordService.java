package com.br.calculator.services;

import com.br.calculator.entities.Record;
import com.br.calculator.entities.User;
import com.br.calculator.repositories.RecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RecordService {

    private final RecordRepository recordRepository;

    public RecordService(RecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    public Optional<List<Record>> findRecordsByUser(User user) {
        return recordRepository.findAllByUser(user);
    }

}
