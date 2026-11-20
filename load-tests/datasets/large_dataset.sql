-- Large synthetic dataset for the Phase 2 load tests (docs/07, section 7.2).
--
-- Run it through seed_large_dataset.sh, or directly with psql:
--   psql -v ON_ERROR_STOP=1 -v trips=100000 -f large_dataset.sql
--
-- Every generated row is tagged so large_dataset_cleanup.sql can remove it again:
--   users      email         lt-user-NNNNN@loadtest.example   (password: LoadTest#2026)
--   operators  name          LT Operator NNN
--   locations  name          LT Station NNN
--   vehicles   license_plate LTVNNNNN (normal), LTHOTN (hot trips)
--   bookings   booking_code  LT-NNNNNNNNNNNN
--
-- Never run this against a database that holds real data.

\set ON_ERROR_STOP 1

\if :{?users} \else \set users 500 \endif
\if :{?operators} \else \set operators 20 \endif
\if :{?locations} \else \set locations 40 \endif
\if :{?routes} \else \set routes 300 \endif
\if :{?vehicles} \else \set vehicles 500 \endif
\if :{?trips} \else \set trips 100000 \endif
-- Each normal trip gets 0..max_bookings bookings, so the average is max_bookings / 2.
\if :{?max_bookings} \else \set max_bookings 6 \endif
\if :{?hot_trips} \else \set hot_trips 5 \endif
\if :{?hot_capacity} \else \set hot_capacity 1000000 \endif
-- Trips depart from (now - past_days) to (now + future_days).
\if :{?past_days} \else \set past_days 30 \endif
\if :{?future_days} \else \set future_days 60 \endif

BEGIN;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM users WHERE email LIKE 'lt-user-%@loadtest.example')
       OR EXISTS (SELECT 1 FROM vehicles WHERE license_plate LIKE 'LTV%' OR license_plate LIKE 'LTHOT%') THEN
        RAISE EXCEPTION 'Load-test data already exists. Run large_dataset_cleanup.sql first.';
    END IF;
END $$;

-- Same seed, same parameters -> same dataset shape (ids and "now" still differ per run).
DO $$ BEGIN PERFORM setseed(0.42); END $$;

-- Wall-clock "now" as the application sees it, rounded to the hour.
CREATE TEMP TABLE lt_anchor ON COMMIT DROP AS
SELECT date_trunc('hour', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') AS ts;

------------------------------------------------------------------------------
-- Users: verified, active, role USER. JWTs are minted offline by make_tokens.py.
------------------------------------------------------------------------------
INSERT INTO users (full_name, email, password, phone, role_id, status, email_verified)
SELECT 'LT User ' || lpad(g::text, 5, '0'),
       'lt-user-' || lpad(g::text, 5, '0') || '@loadtest.example',
       -- bcrypt (cost 10) of "LoadTest#2026"
       '$2a$10$fHEiPPQ8gm1cfldBvPVW6OJ.zay5e4X7dUbRm60WoAiARECiwQTMu',
       '09' || lpad(g::text, 8, '0'),
       (SELECT id FROM roles WHERE name = 'USER'),
       'ACTIVE',
       TRUE
FROM generate_series(1, :users) g;

CREATE TEMP TABLE lt_users ON COMMIT DROP AS
SELECT id, row_number() OVER (ORDER BY id) AS rn
FROM users WHERE email LIKE 'lt-user-%@loadtest.example';

------------------------------------------------------------------------------
-- Catalog: operators, locations, routes, vehicles
------------------------------------------------------------------------------
INSERT INTO bus_operators (name, address, phone, email, status)
SELECT 'LT Operator ' || lpad(g::text, 3, '0'),
       'LT Address ' || g,
       '08' || lpad(g::text, 8, '0'),
       'lt-operator-' || lpad(g::text, 3, '0') || '@loadtest.example',
       'ACTIVE'
FROM generate_series(1, :operators) g;

INSERT INTO locations (name, province, address)
SELECT 'LT Station ' || lpad(g::text, 3, '0'),
       'LT Province ' || lpad(((g - 1) / 2 + 1)::text, 3, '0'),
       'LT Address ' || g
FROM generate_series(1, :locations) g;

-- Distinct (origin, destination) pairs in a shuffled order.
INSERT INTO routes (origin_id, destination_id, estimated_duration_minutes, status)
SELECT origin_id, destination_id, 60 + (random() * 600)::int, 'ACTIVE'
FROM (
    SELECT a.id AS origin_id, b.id AS destination_id
    FROM locations a
    JOIN locations b ON a.id <> b.id
    WHERE a.name LIKE 'LT Station %' AND b.name LIKE 'LT Station %'
    ORDER BY random()
    LIMIT :routes
) pairs;

-- rn = 1 is the most popular route (see the skew in the trips insert).
CREATE TEMP TABLE lt_routes ON COMMIT DROP AS
SELECT r.id, r.estimated_duration_minutes AS duration, row_number() OVER (ORDER BY r.id) AS rn
FROM routes r
JOIN locations o ON o.id = r.origin_id
WHERE o.name LIKE 'LT Station %';

INSERT INTO vehicles (operator_id, license_plate, vehicle_type, seat_capacity, status)
SELECT (SELECT id FROM bus_operators WHERE name = 'LT Operator ' || lpad(((g - 1) % :operators + 1)::text, 3, '0')),
       'LTV' || lpad(g::text, 5, '0'),
       'BUS',
       (ARRAY[16, 29, 34, 45])[1 + g % 4],
       'ACTIVE'
FROM generate_series(1, :vehicles) g;

CREATE TEMP TABLE lt_vehicles ON COMMIT DROP AS
SELECT id, seat_capacity, row_number() OVER (ORDER BY id) AS rn
FROM vehicles WHERE license_plate LIKE 'LTV%';

------------------------------------------------------------------------------
-- Trips: skewed towards popular routes, spread over past and future days.
-- Past trips are COMPLETED, future trips SCHEDULED, about 5% are CANCELLED.
------------------------------------------------------------------------------
CREATE TEMP TABLE lt_trip_plan ON COMMIT DROP AS
SELECT g,
       1 + floor(power(random(), 2) * (SELECT count(*) FROM lt_routes))::int AS route_rn,
       1 + floor(random() * (SELECT count(*) FROM lt_vehicles))::int AS vehicle_rn,
       (SELECT ts FROM lt_anchor)
           + make_interval(days => floor(random() * (:past_days + :future_days))::int - :past_days,
                           hours => floor(random() * 24)::int,
                           mins => 15 * floor(random() * 4)::int) AS departure_time,
       (50 + floor(random() * 46)::int * 10) * 1000 AS price,
       random() < 0.05 AS cancelled
FROM generate_series(1, :trips) g;

INSERT INTO trips (route_id, vehicle_id, departure_time, arrival_time, price, available_seats, status, created_at)
SELECT r.id,
       v.id,
       p.departure_time,
       p.departure_time + make_interval(mins => r.duration),
       p.price,
       v.seat_capacity,
       CASE WHEN p.cancelled THEN 'CANCELLED'
            WHEN p.departure_time < (SELECT ts FROM lt_anchor) THEN 'COMPLETED'
            ELSE 'SCHEDULED' END,
       LEAST(p.departure_time, (SELECT ts FROM lt_anchor)) - interval '30 days'
FROM lt_trip_plan p
JOIN lt_routes r ON r.rn = p.route_rn
JOIN lt_vehicles v ON v.rn = p.vehicle_rn;

------------------------------------------------------------------------------
-- Bookings on non-cancelled trips. At most max_bookings * 2 seats per trip,
-- capped so a trip is never oversold.
------------------------------------------------------------------------------
CREATE TEMP TABLE lt_booking_plan ON COMMIT DROP AS
SELECT t.id AS trip_id, t.price, t.departure_time, v.seat_capacity,
       floor(random() * (:max_bookings + 1))::int AS booking_count
FROM trips t
JOIN lt_vehicles v ON v.id = t.vehicle_id
WHERE t.status <> 'CANCELLED';

INSERT INTO bookings (booking_code, user_id, trip_id, passenger_name, passenger_phone,
                      quantity, unit_price, total_amount, status, booked_at)
SELECT 'LT-' || lpad((row_number() OVER ())::text, 12, '0'),
       u.id,
       b.trip_id,
       'LT Passenger ' || b.user_rn,
       '09' || lpad(b.user_rn::text, 8, '0'),
       b.quantity,
       b.price,
       b.price * b.quantity,
       b.status,
       LEAST(b.departure_time, (SELECT ts FROM lt_anchor)) - make_interval(mins => b.minutes_before)
FROM (
    SELECT p.trip_id, p.price, p.departure_time,
           1 + floor(random() * (SELECT count(*) FROM lt_users))::int AS user_rn,
           CASE WHEN random() < 0.3 THEN 2 ELSE 1 END AS quantity,
           CASE WHEN random() < 0.1 THEN 'CANCELLED' ELSE 'CONFIRMED' END AS status,
           1 + floor(random() * 20 * 24 * 60)::int AS minutes_before
    FROM lt_booking_plan p
    CROSS JOIN LATERAL generate_series(1, LEAST(p.booking_count, p.seat_capacity / 2)) k
) b
JOIN lt_users u ON u.rn = b.user_rn;

-- Keep the invariant: available_seats + confirmed seats = seat capacity.
UPDATE trips t
SET available_seats = p.seat_capacity - s.seats
FROM lt_booking_plan p
JOIN (
    SELECT trip_id, sum(quantity) AS seats
    FROM bookings
    WHERE status = 'CONFIRMED' AND booking_code LIKE 'LT-%'
    GROUP BY trip_id
) s ON s.trip_id = p.trip_id
WHERE t.id = p.trip_id;

------------------------------------------------------------------------------
-- Hot trips: few SCHEDULED trips with a very large capacity and no bookings,
-- used by the "hot trip" booking scenario.
------------------------------------------------------------------------------
INSERT INTO vehicles (operator_id, license_plate, vehicle_type, seat_capacity, status)
SELECT (SELECT min(id) FROM bus_operators WHERE name LIKE 'LT Operator %'),
       'LTHOT' || g,
       'BUS',
       :hot_capacity,
       'ACTIVE'
FROM generate_series(1, :hot_trips) g;

INSERT INTO trips (route_id, vehicle_id, departure_time, arrival_time, price, available_seats, status, created_at)
SELECT r.id,
       v.id,
       (SELECT ts FROM lt_anchor) + interval '30 days',
       (SELECT ts FROM lt_anchor) + interval '30 days' + make_interval(mins => r.duration),
       150000,
       v.seat_capacity,
       'SCHEDULED',
       (SELECT ts FROM lt_anchor)
FROM vehicles v
JOIN lt_routes r ON r.rn = 1
WHERE v.license_plate LIKE 'LTHOT%';

DO $$
DECLARE
    broken bigint;
BEGIN
    SELECT count(*) INTO broken
    FROM trips t
    JOIN vehicles v ON v.id = t.vehicle_id
    LEFT JOIN (
        SELECT trip_id, sum(quantity) AS seats
        FROM bookings WHERE status = 'CONFIRMED' GROUP BY trip_id
    ) s ON s.trip_id = t.id
    WHERE (v.license_plate LIKE 'LTV%' OR v.license_plate LIKE 'LTHOT%')
      AND t.available_seats + COALESCE(s.seats, 0) <> v.seat_capacity;
    IF broken > 0 THEN
        RAISE EXCEPTION 'Seat balance is wrong on % generated trips', broken;
    END IF;
END $$;

COMMIT;

ANALYZE users;
ANALYZE bus_operators;
ANALYZE locations;
ANALYZE routes;
ANALYZE vehicles;
ANALYZE trips;
ANALYZE bookings;

SELECT (SELECT count(*) FROM users WHERE email LIKE 'lt-user-%@loadtest.example') AS users,
       (SELECT count(*) FROM routes r JOIN locations o ON o.id = r.origin_id
         WHERE o.name LIKE 'LT Station %') AS routes,
       (SELECT count(*) FROM trips t JOIN vehicles v ON v.id = t.vehicle_id
         WHERE v.license_plate LIKE 'LTV%') AS trips,
       (SELECT count(*) FROM trips t JOIN vehicles v ON v.id = t.vehicle_id
         WHERE v.license_plate LIKE 'LTHOT%') AS hot_trips,
       (SELECT count(*) FROM bookings WHERE booking_code LIKE 'LT-%') AS bookings;
