package com.example.busbooking.business.port.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.busbooking.domain.enums.OperatorStatus;
import com.example.busbooking.domain.model.Operator;
import java.util.Optional;

public interface OperatorRepository {
    Page<Operator> search(String query, OperatorStatus status, Pageable pageable);
    Optional<Operator> findByIdForUpdate(Long id);
    boolean existsByName(String name, Long excludedId);
    boolean existsByEmail(String email, Long excludedId);
    boolean hasOperationalDependencies(Long id);
    Operator save(Operator operator);
}
