-- Removes everything created by large_dataset.sql, plus bookings that load tests
-- made on the generated trips or with the generated users.
--
-- The foreign keys into bookings and trips have no supporting index, so each
-- deleted parent row would scan the whole child table. The lt_tmp_* indexes make
-- those checks fast and are dropped again before commit: the schema is unchanged.

\set ON_ERROR_STOP 1

BEGIN;

CREATE TEMP TABLE lt_trip_ids ON COMMIT DROP AS
SELECT t.id
FROM trips t
JOIN vehicles v ON v.id = t.vehicle_id
WHERE v.license_plate LIKE 'LTV%' OR v.license_plate LIKE 'LTHOT%';

CREATE TEMP TABLE lt_user_ids ON COMMIT DROP AS
SELECT id FROM users
WHERE email LIKE 'lt-user-%@loadtest.example'
   -- accounts created by register_locust.py
   OR email LIKE 'lt-reg-%@loadtest.example';

DELETE FROM bookings
WHERE trip_id IN (SELECT id FROM lt_trip_ids)
   OR user_id IN (SELECT id FROM lt_user_ids);

CREATE INDEX lt_tmp_bookings_trip_id ON bookings (trip_id);
CREATE INDEX lt_tmp_bookings_user_id ON bookings (user_id);

DELETE FROM trips WHERE id IN (SELECT id FROM lt_trip_ids);

CREATE INDEX lt_tmp_trips_vehicle_id ON trips (vehicle_id);
CREATE INDEX lt_tmp_trips_route_id ON trips (route_id);

DELETE FROM vehicles WHERE license_plate LIKE 'LTV%' OR license_plate LIKE 'LTHOT%';

DELETE FROM routes r
USING locations o
WHERE o.id = r.origin_id AND o.name LIKE 'LT Station %';

DELETE FROM locations WHERE name LIKE 'LT Station %';

DELETE FROM bus_operators WHERE name LIKE 'LT Operator %';

-- email_verifications rows, if any, go with ON DELETE CASCADE.
DELETE FROM users WHERE id IN (SELECT id FROM lt_user_ids);

DROP INDEX lt_tmp_bookings_trip_id;
DROP INDEX lt_tmp_bookings_user_id;
DROP INDEX lt_tmp_trips_vehicle_id;
DROP INDEX lt_tmp_trips_route_id;

COMMIT;

ANALYZE trips;
ANALYZE bookings;
