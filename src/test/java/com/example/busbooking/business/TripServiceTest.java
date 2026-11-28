
package com.example.busbooking.business;

import com.example.busbooking.business.port.repository.TripRepository;
import com.example.busbooking.business.service.TripService;
import com.example.busbooking.domain.exception.ResourceNotFoundException;
import com.example.busbooking.domain.model.Trip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final PageRequest PAGE = PageRequest.of(1, 10);

    @Mock
    private TripRepository tripRepository;

    @InjectMocks
    private TripService tripService;

    @Test
    void searchTrips_whenTripsExist_shouldReturnMatchingTrips() {
        Long originId = 1L;
        Long destinationId = 2L;
        LocalDate departureDate = LocalDate.of(2026, 10, 25);
        Trip trip = mock(Trip.class);
        List<Trip> expectedTrips = List.of(trip);

        when(tripRepository.search(originId, destinationId, departureDate, PAGE))
                .thenReturn(new PageImpl<>(expectedTrips, PAGE, 11));

        Page<Trip> actualTrips = tripService.searchTrips(
                originId, destinationId, departureDate, PAGE
        );

        assertEquals(expectedTrips, actualTrips.getContent());
        assertEquals(11, actualTrips.getTotalElements());
        assertEquals(1, actualTrips.getNumber());
        verify(tripRepository).search(originId, destinationId, departureDate, PAGE);
    }

    @Test
    void searchTrips_whenNoTripsExist_shouldReturnEmptyList() {
        Long originId = 1L;
        Long destinationId = 2L;
        LocalDate departureDate = LocalDate.of(2026, 10, 25);

        when(tripRepository.search(originId, destinationId, departureDate, PAGE))
                .thenReturn(Page.empty(PAGE));

        Page<Trip> result = tripService.searchTrips(
                originId, destinationId, departureDate, PAGE
        );

        assertTrue(result.isEmpty());
        verify(tripRepository).search(originId, destinationId, departureDate, PAGE);
    }

    @Test
    void searchTrips_whenAllFiltersAreNull_shouldDelegateNullsWithoutChangingThem() {
        when(tripRepository.search(null, null, null, PAGE)).thenReturn(Page.empty(PAGE));

        assertTrue(tripService.searchTrips(null, null, null, PAGE).isEmpty());

        verify(tripRepository).search(null, null, null, PAGE);
        verifyNoMoreInteractions(tripRepository);
    }

    @Test
    void getTripById_whenTripExists_shouldReturnTrip() {
        Long tripId = 10L;
        Trip expectedTrip = mock(Trip.class);

        when(tripRepository.findById(tripId))
                .thenReturn(Optional.of(expectedTrip));

        Trip actualTrip = tripService.getTripById(tripId);

        assertSame(expectedTrip, actualTrip);
        verify(tripRepository).findById(tripId);
    }

    @Test
    void getTripById_whenTripDoesNotExist_shouldThrowResourceNotFoundException() {
        Long tripId = 99L;

        when(tripRepository.findById(tripId))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> tripService.getTripById(tripId)
        );

        assertEquals("Trip not found with id: 99", exception.getMessage());
        verify(tripRepository).findById(tripId);
    }
}
