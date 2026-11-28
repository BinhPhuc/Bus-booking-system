
package com.example.busbooking.data.repository;

import com.example.busbooking.data.entity.TripEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SpringDataTripRepository
        extends JpaRepository<TripEntity, Long> {

    @Query(value = """
        SELECT t FROM TripEntity t
        JOIN FETCH t.route r
        JOIN FETCH r.origin
        JOIN FETCH r.destination
        JOIN FETCH t.vehicle v
        JOIN FETCH v.operator
        WHERE (:skipRoute = true OR t.routeId = :routeId)
          AND (:skipStatus = true OR t.status = :status)
          AND (:skipDate = true OR (t.departureTime >= :start AND t.departureTime < :end))
        ORDER BY t.departureTime ASC, t.id ASC
        """, countQuery = """
        SELECT count(t) FROM TripEntity t
        JOIN t.route r
        JOIN r.origin
        JOIN r.destination
        JOIN t.vehicle v
        JOIN v.operator
        WHERE (:skipRoute = true OR t.routeId = :routeId)
          AND (:skipStatus = true OR t.status = :status)
          AND (:skipDate = true OR (t.departureTime >= :start AND t.departureTime < :end))
        """)
    Page<TripEntity> findForAdmin(Long routeId, com.example.busbooking.domain.enums.TripStatus status,
                                  LocalDateTime start, LocalDateTime end,
                                  boolean skipRoute, boolean skipStatus, boolean skipDate, Pageable pageable);

    default Page<TripEntity> search(
            Long originId,
            Long destinationId,
            LocalDateTime departureDate,
            LocalDateTime departureDateEnd,
            LocalDateTime now,
            Pageable pageable
    ) {
        if ((departureDate == null) != (departureDateEnd == null)) {
            throw new IllegalArgumentException(
                    "Departure date range must have both bounds"
            );
        }

        return searchFiltered(
                originId,
                destinationId,
                departureDate,
                departureDateEnd,
                now,
                originId == null,
                destinationId == null,
                departureDate == null,
                pageable
        );
    }

    @Query(value = """
        SELECT t
        FROM TripEntity t
        JOIN FETCH t.route r
        JOIN FETCH r.origin
        JOIN FETCH r.destination
        JOIN FETCH t.vehicle v
        JOIN FETCH v.operator
        WHERE t.status = com.example.busbooking.domain.enums.TripStatus.SCHEDULED
          AND t.departureTime > :now
          AND (:skipOrigin = true OR r.origin.id = :originId)
          AND (:skipDestination = true OR r.destination.id = :destinationId)
          AND (
              :skipDate = true
              OR (
                  t.departureTime >= :departureDate
                  AND t.departureTime < :departureDateEnd
              )
          )
        ORDER BY t.departureTime ASC, t.id ASC
        """, countQuery = """
        SELECT count(t)
        FROM TripEntity t
        JOIN t.route r
        JOIN r.origin
        JOIN r.destination
        JOIN t.vehicle v
        JOIN v.operator
        WHERE t.status = com.example.busbooking.domain.enums.TripStatus.SCHEDULED
          AND t.departureTime > :now
          AND (:skipOrigin = true OR r.origin.id = :originId)
          AND (:skipDestination = true OR r.destination.id = :destinationId)
          AND (
              :skipDate = true
              OR (
                  t.departureTime >= :departureDate
                  AND t.departureTime < :departureDateEnd
              )
          )
        """)
    Page<TripEntity> searchFiltered(
            @Param("originId") Long originId,
            @Param("destinationId") Long destinationId,
            @Param("departureDate") LocalDateTime departureDate,
            @Param("departureDateEnd") LocalDateTime departureDateEnd,
            @Param("now") LocalDateTime now,
            @Param("skipOrigin") boolean skipOrigin,
            @Param("skipDestination") boolean skipDestination,
            @Param("skipDate") boolean skipDate,
            Pageable pageable
    );

    @Query("""
        SELECT t
        FROM TripEntity t
        JOIN FETCH t.route r
        JOIN FETCH r.origin
        JOIN FETCH r.destination
        JOIN FETCH t.vehicle v
        JOIN FETCH v.operator
        WHERE t.id = :tripId
        """)
    Optional<TripEntity> findDetailedById(
            @Param("tripId") Long tripId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    // Lock the root row before loading associations. A joined locking query can lose the row
    // during PostgreSQL's recheck if a concurrent update changes its vehicle foreign key.
    @Query("""
        SELECT t
        FROM TripEntity t
        WHERE t.id = :tripId
        """)
    Optional<TripEntity> findByIdForUpdate(
            @Param("tripId") Long tripId
    );

    @Modifying
    @Query("""
        UPDATE TripEntity t
        SET t.availableSeats = t.availableSeats + :seatDelta
        WHERE t.id = :tripId
          AND t.availableSeats + :seatDelta >= 0
        """)
    int updateAvailableSeats(
            @Param("tripId") Long tripId,
            @Param("seatDelta") int seatDelta
    );
}
