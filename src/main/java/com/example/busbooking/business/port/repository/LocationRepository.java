package com.example.busbooking.business.port.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.busbooking.domain.model.Location;
import java.util.Optional;

public interface LocationRepository {
    Page<Location> search(String query, Pageable pageable);
    Optional<Location> findById(Long id);
    boolean existsByIdentity(String name, String province, String address, Long excludedId);
    Location save(Location location);
}
