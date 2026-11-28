FIRST_PAGE = 0
PAGE_LIMIT = 20


def get_trip_page(client, name, **filters):
    with client.get(
        "/api/trips",
        params={**filters, "page": FIRST_PAGE, "limit": PAGE_LIMIT},
        name=name,
        catch_response=True,
    ) as response:
        if 200 < response.status_code < 400:
            response.failure(f"Expected HTTP 200 for trip list, got {response.status_code}")
            return
        if response.status_code != 200:
            return

        try:
            body = response.json()
        except ValueError:
            response.failure("Trip list response is not valid JSON")
            return

        if not isinstance(body, dict):
            response.failure("Expected a paginated trip list object")
            return

        content = body.get("content")
        if not isinstance(content, list) or len(content) > PAGE_LIMIT:
            response.failure("Invalid trip page content")
            return

        metadata = ("page", "limit", "totalElements", "totalPages")
        if (
            any(type(body.get(key)) is not int or body[key] < 0 for key in metadata)
            or body["page"] != FIRST_PAGE
            or body["limit"] != PAGE_LIMIT
        ):
            response.failure("Invalid trip pagination metadata")
