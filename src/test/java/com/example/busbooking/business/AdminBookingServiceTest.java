package com.example.busbooking.business;

import com.example.busbooking.business.port.repository.*;
import com.example.busbooking.business.service.BookingService;
import com.example.busbooking.domain.enums.BookingStatus;
import com.example.busbooking.domain.model.Booking;
import org.junit.jupiter.api.Test;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminBookingServiceTest {

    private static final PageRequest PAGE = PageRequest.of(1, 10);
    BookingRepository repository = mock(BookingRepository.class);
    BookingService service = new BookingService(repository, mock(TripRepository.class),
            new io.micrometer.core.instrument.simple.SimpleMeterRegistry());

    @Test void filtersAreDelegatedAndResultsAreNotRestrictedToCurrentUser() {
        Booking booking = mock(Booking.class);
        when(repository.findForAdmin(7L, 9L, BookingStatus.CONFIRMED, PAGE)).thenReturn(new PageImpl<>(List.of(booking), PAGE, 11));
        assertThat(service.listForAdmin(7L, 9L, BookingStatus.CONFIRMED, PAGE)).containsExactly(booking);
        verify(repository).findForAdmin(7L, 9L, BookingStatus.CONFIRMED, PAGE);
    }

    @Test void noFiltersRequestsBookingsAcrossAllUsers() {
        when(repository.findForAdmin(null, null, null, PAGE)).thenReturn(Page.empty(PAGE));
        assertThat(service.listForAdmin(null, null, null, PAGE)).isEmpty();
        verify(repository).findForAdmin(null, null, null, PAGE);
    }
}
