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

    Optional<Record> findByIdAndActive(Long id, Boolean active);

    Optional<List<Record>> findAllByUserAndActive(User user, Boolean active);
    Page<Record> findAllByUserAndActive(User user, Boolean active, Pageable pageable);
}
