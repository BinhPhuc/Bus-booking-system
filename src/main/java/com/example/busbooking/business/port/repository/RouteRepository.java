package com.example.busbooking.business.port.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.busbooking.domain.model.Route;
import com.example.busbooking.domain.enums.RouteStatus;
import java.util.Optional;

public interface RouteRepository {
    Page<Route> search(Long originId, Long destinationId, RouteStatus status, Pageable pageable);
    Optional<Route> findById(Long id);
    Optional<Route> findByIdForUpdate(Long id);
    boolean existsByEndpoints(Long originId, Long destinationId, Long excludedId);
    boolean hasAnyTrips(Long id);
    boolean hasOperationalTrips(Long id);
    Route save(Route route);
}
