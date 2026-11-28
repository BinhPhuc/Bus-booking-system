package com.example.busbooking.business.port.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.busbooking.domain.model.Trip;

import java.time.LocalDate;
import java.util.Optional;

public interface TripRepository {
    Page<Trip> findForAdmin(Long routeId, com.example.busbooking.domain.enums.TripStatus status, LocalDate departureDate, Pageable pageable);

    Trip save(Trip trip);

    Page<Trip> search(
            Long originId,
            Long destinationId,
            LocalDate departureDate,
            Pageable pageable);

    Optional<Trip> findById(Long tripId);

    Optional<Trip> findByIdForUpdate(Long tripId);

    boolean updateAvailableSeats(Long tripId, int seatDelta);
}
