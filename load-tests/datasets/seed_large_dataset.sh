#!/usr/bin/env bash
# Loads the large load-test dataset into PostgreSQL and writes a manifest with the
# ids the Locust scenarios need.
#
#   ./seed_large_dataset.sh            generate (fails if load-test data already exists)
#   ./seed_large_dataset.sh --clean    remove the generated data
#   ./seed_large_dataset.sh --reset    remove, then generate again
#
# Connection (defaults match compose.yaml):
#   PG_CONTAINER=busbooking PG_USER=root PG_DATABASE=busbooking
# Set PSQL to use another client, for example:
#   PSQL="psql -h localhost -p 5432 -U root -d busbooking" ./seed_large_dataset.sh
#
# Size (defaults: 100000 trips, about 300000 bookings, 500 users):
#   TRIPS=100000 MAX_BOOKINGS=6 USERS=500 ROUTES=300 LOCATIONS=40 VEHICLES=500
#   HOT_TRIPS=5 HOT_CAPACITY=1000000
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
out_dir="$here/generated"
manifest="$out_dir/large_dataset_manifest.json"

PG_CONTAINER="${PG_CONTAINER:-busbooking}"
PG_USER="${PG_USER:-root}"
PG_DATABASE="${PG_DATABASE:-busbooking}"
PSQL="${PSQL:-docker exec -i $PG_CONTAINER psql -U $PG_USER -d $PG_DATABASE}"

run_sql() {
    local file="$1"
    shift
    # shellcheck disable=SC2086
    $PSQL -v ON_ERROR_STOP=1 "$@" < "$file"
}

clean() {
    echo "Removing load-test data..."
    run_sql "$here/large_dataset_cleanup.sql" -q
}

generate() {
    echo "Generating load-test data (this takes a moment)..."
    run_sql "$here/large_dataset.sql" -q \
        -v users="${USERS:-500}" \
        -v operators="${OPERATORS:-20}" \
        -v locations="${LOCATIONS:-40}" \
        -v routes="${ROUTES:-300}" \
        -v vehicles="${VEHICLES:-500}" \
        -v trips="${TRIPS:-100000}" \
        -v max_bookings="${MAX_BOOKINGS:-6}" \
        -v hot_trips="${HOT_TRIPS:-5}" \
        -v hot_capacity="${HOT_CAPACITY:-1000000}"

    mkdir -p "$out_dir"
    # shellcheck disable=SC2086
    $PSQL -v ON_ERROR_STOP=1 -At > "$manifest" <<'SQL'
SELECT json_build_object(
    'user_ids', (SELECT json_agg(id ORDER BY id) FROM users
                 WHERE email LIKE 'lt-user-%@loadtest.example'),
    'hot_trip_ids', (SELECT json_agg(t.id ORDER BY t.id) FROM trips t
                     JOIN vehicles v ON v.id = t.vehicle_id
                     WHERE v.license_plate LIKE 'LTHOT%'),
    'sample_trip_id', (SELECT min(t.id) FROM trips t
                       JOIN vehicles v ON v.id = t.vehicle_id
                       WHERE v.license_plate LIKE 'LTV%' AND t.status = 'SCHEDULED'),
    -- Popular (origin, destination) pairs for filtered searches.
    'search_routes', (SELECT json_agg(json_build_object(
                                 'originId', origin_id, 'destinationId', destination_id))
                      FROM (SELECT r.origin_id, r.destination_id
                            FROM trips t
                            JOIN routes r ON r.id = t.route_id
                            JOIN vehicles v ON v.id = t.vehicle_id
                            WHERE v.license_plate LIKE 'LTV%'
                            GROUP BY r.origin_id, r.destination_id
                            ORDER BY count(*) DESC
                            LIMIT 20) top)
);
SQL
    echo "Manifest written to $manifest"
}

case "${1:-}" in
    "") generate ;;
    --clean) clean ;;
    --reset) clean; generate ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
esac
