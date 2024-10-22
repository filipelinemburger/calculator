package com.br.calculator.repositories;

import com.br.calculator.entities.Record;
import com.br.calculator.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RecordRepository extends JpaRepository<Record, Long> {
    Optional<List<Record>> findAllByUser(User user);
    Page<Record> findAllByUser(User user, Pageable pageable);
}
