CREATE INDEX idx_trips_route_id_departure_time ON trips (route_id, departure_time);
CREATE INDEX idx_bookings_user_id ON bookings (user_id);
CREATE INDEX idx_bookings_trip_id ON bookings (trip_id);
