"""Phase 2 write scenario: every booking goes to a few "hot" trips.

80% of the virtual users book one seat on a hot trip, each with its own account.
20% read a trip detail at the same time, to show whether booking contention
slows down unrelated requests.

    BUSBOOKING_MANIFEST=load-tests/datasets/generated/large_dataset_manifest.json
    BUSBOOKING_TOKENS=load-tests/datasets/generated/tokens.json
    BUSBOOKING_HOT_TRIP_COUNT=1     how many hot trips share the load (default 1)
"""
import itertools
import json
import os
import random

from locust import HttpUser, task, between

with open(os.environ["BUSBOOKING_MANIFEST"], encoding="utf-8") as handle:
    MANIFEST = json.load(handle)
with open(os.environ["BUSBOOKING_TOKENS"], encoding="utf-8") as handle:
    TOKENS = json.load(handle)

HOT_TRIP_IDS = MANIFEST["hot_trip_ids"][: int(os.environ.get("BUSBOOKING_HOT_TRIP_COUNT", "1"))]
READ_TRIP_ID = MANIFEST["sample_trip_id"]
token_cycle = itertools.cycle(TOKENS)


class HotTripBookingUser(HttpUser):
    weight = 4
    wait_time = between(0.2, 0.6)

    def on_start(self):
        self.headers = {"Authorization": f"Bearer {next(token_cycle)}"}

    @task
    def create_booking(self):
        with self.client.post(
            "/api/bookings",
            json={
                "tripId": random.choice(HOT_TRIP_IDS),
                "passengerName": "Load Tester",
                "passengerPhone": "0901234567",
                "quantity": 1,
            },
            headers=self.headers,
            name="POST /api/bookings (hot trip)",
            catch_response=True,
            timeout=15,
        ) as response:
            if response.status_code == 201:
                response.success()
            else:
                response.failure(f"HTTP {response.status_code}")


class BackgroundReader(HttpUser):
    weight = 1
    wait_time = between(0.2, 0.6)

    @task
    def trip_detail(self):
        self.client.get(
            f"/api/trips/{READ_TRIP_ID}",
            name="GET /api/trips/{id} (during hot trip)",
            timeout=15,
        )
