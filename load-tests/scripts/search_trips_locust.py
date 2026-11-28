"""Phase 2 read scenario: search trips the way a passenger does.

75% searches by origin, destination and departure date on popular routes,
25% trip detail. Needs the manifest written by seed_large_dataset.sh:

    BUSBOOKING_MANIFEST=load-tests/datasets/generated/large_dataset_manifest.json
"""
import datetime
import json
import os
import random

from locust import HttpUser, between, task

from trip_page import get_trip_page

with open(os.environ["BUSBOOKING_MANIFEST"], encoding="utf-8") as handle:
    MANIFEST = json.load(handle)

ROUTES = MANIFEST["search_routes"]
TRIP_ID = MANIFEST["sample_trip_id"]
VIETNAM_TIME = datetime.timezone(datetime.timedelta(hours=7))


class TripSearchUser(HttpUser):
    wait_time = between(0.2, 0.6)

    @task(3)
    def search(self):
        route = random.choice(ROUTES)
        today = datetime.datetime.now(VIETNAM_TIME).date()
        date = today + datetime.timedelta(days=random.randint(1, 30))
        get_trip_page(
            self.client,
            name="GET /api/trips?origin&destination&date",
            originId=route["originId"],
            destinationId=route["destinationId"],
            departureDate=date.isoformat(),
        )

    @task(1)
    def trip_detail(self):
        self.client.get(f"/api/trips/{TRIP_ID}", name="GET /api/trips/{id}")
