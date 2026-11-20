"""Phase 2 scenario: registration, which sends the OTP email inside the request.

Run it once with a working SMTP server and once with a failing one
(see smtp_blackhole.py). The backend must run with a raised
AUTH_RATE_REGISTER_LIMIT, otherwise almost every request is answered 429.
"""
import random
import uuid

from locust import HttpUser, task, between


class RegisterUser(HttpUser):
    wait_time = between(0.2, 0.6)

    @task
    def register(self):
        with self.client.post(
            "/api/auth/register",
            json={
                "fullName": "Load Test Register",
                "email": f"lt-reg-{uuid.uuid4().hex}@loadtest.example",
                "password": "LoadTest#2026",
                "phone": "09" + "".join(random.choices("0123456789", k=8)),
            },
            name="POST /api/auth/register",
            catch_response=True,
            timeout=30,
        ) as response:
            if response.status_code == 201:
                response.success()
            else:
                response.failure(f"HTTP {response.status_code}")
