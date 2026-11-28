package com.example.busbooking.data;

import com.example.busbooking.data.adapter.TripRepositoryAdapter;
import com.example.busbooking.data.entity.TripEntity;
import com.example.busbooking.data.mapper.TripPersistenceMapper;
import com.example.busbooking.data.repository.SpringDataTripRepository;
import com.example.busbooking.domain.model.Trip;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripRepositoryAdapterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T20:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 25, 3, 0);
    private static final PageRequest PAGE = PageRequest.of(1, 10);

    @Mock
    private SpringDataTripRepository repository;

    @Mock
    private TripPersistenceMapper mapper;

    @Mock
    private Trip trip;

    @Test
    void search_withoutDatePassesBothDateBoundsAsNull() {
        when(repository.search(null, 2L, null, null, NOW, PAGE)).thenReturn(Page.empty(PAGE));
        TripRepositoryAdapter adapter = new TripRepositoryAdapter(repository, mapper, CLOCK);

        assertEquals(List.of(), adapter.search(null, 2L, null, PAGE).getContent());

        verify(repository).search(null, 2L, null, null, NOW, PAGE);
        verifyNoInteractions(mapper);
    }

    @Test
    void search_withDateUsesHalfOpenWholeDayRange() {
        TripEntity entity = mock(TripEntity.class);
        when(repository.search(eq(1L), eq(2L),
                eq(LocalDate.of(2026, 10, 25).atStartOfDay()),
                eq(LocalDate.of(2026, 10, 26).atStartOfDay()), eq(NOW), eq(PAGE)))
                .thenReturn(new PageImpl<>(List.of(entity), PAGE, 11));
        when(mapper.toDomain(entity)).thenReturn(trip);
        TripRepositoryAdapter adapter = new TripRepositoryAdapter(repository, mapper, CLOCK);

        assertEquals(List.of(trip), adapter.search(1L, 2L, LocalDate.of(2026, 10, 25), PAGE).getContent());

        verify(mapper).toDomain(entity);
    }
}
