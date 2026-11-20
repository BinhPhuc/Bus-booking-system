"""Phase 2 read scenario: search trips the way a passenger does.

75% searches by origin, destination and departure date on popular routes,
25% trip detail. Needs the manifest written by seed_large_dataset.sh:

    BUSBOOKING_MANIFEST=load-tests/datasets/generated/large_dataset_manifest.json
"""
import datetime
import json
import os
import random

from locust import HttpUser, task, between

with open(os.environ["BUSBOOKING_MANIFEST"], encoding="utf-8") as handle:
    MANIFEST = json.load(handle)

ROUTES = MANIFEST["search_routes"]
TRIP_ID = MANIFEST["sample_trip_id"]
TODAY = datetime.date.today()


class TripSearchUser(HttpUser):
    wait_time = between(0.2, 0.6)

    @task(3)
    def search(self):
        route = random.choice(ROUTES)
        date = TODAY + datetime.timedelta(days=random.randint(1, 30))
        self.client.get(
            "/api/trips",
            params={
                "originId": route["originId"],
                "destinationId": route["destinationId"],
                "departureDate": date.isoformat(),
            },
            name="GET /api/trips?origin&destination&date",
        )

    @task(1)
    def trip_detail(self):
        self.client.get(f"/api/trips/{TRIP_ID}", name="GET /api/trips/{id}")
