"""Mint access JWTs for the generated load-test users, without calling the login API.

The auth rate limiter allows only a few logins per minute from one IP, so the
tokens are signed locally with the same key and claims as JwtService.

Usage (JWT_SECRET must be the value the backend runs with):

    JWT_SECRET=... python3 load-tests/scripts/make_tokens.py

Reads  load-tests/datasets/generated/large_dataset_manifest.json
Writes load-tests/datasets/generated/tokens.json   (a JSON list of tokens)

The output contains valid credentials for the target environment: never commit it.
"""
import argparse
import base64
import hashlib
import hmac
import json
import os
import sys
import time
from pathlib import Path

GENERATED = Path(__file__).resolve().parent.parent / "datasets" / "generated"


def b64url(raw: bytes) -> str:
    return base64.urlsafe_b64encode(raw).rstrip(b"=").decode("ascii")


def sign(user_id: int, key: bytes, issuer: str, issued_at: int, ttl_seconds: int) -> str:
    header = {"alg": "HS256"}
    claims = {"sub": str(user_id), "iss": issuer, "iat": issued_at, "exp": issued_at + ttl_seconds}
    signing_input = ".".join(
        b64url(json.dumps(part, separators=(",", ":")).encode("utf-8")) for part in (header, claims)
    )
    signature = hmac.new(key, signing_input.encode("ascii"), hashlib.sha256).digest()
    return f"{signing_input}.{b64url(signature)}"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--manifest", type=Path, default=GENERATED / "large_dataset_manifest.json")
    parser.add_argument("--output", type=Path, default=GENERATED / "tokens.json")
    parser.add_argument("--issuer", default=os.environ.get("JWT_ISSUER", "busbooking"))
    parser.add_argument("--ttl-hours", type=float, default=24.0)
    args = parser.parse_args()

    secret = os.environ.get("JWT_SECRET")
    if not secret:
        print("JWT_SECRET is not set", file=sys.stderr)
        return 1
    try:
        key = base64.b64decode(secret, validate=True)
    except ValueError:
        print("JWT_SECRET must be valid Base64", file=sys.stderr)
        return 1
    if len(key) < 32:
        print("JWT_SECRET requires at least 32 decoded bytes", file=sys.stderr)
        return 1

    user_ids = json.loads(args.manifest.read_text())["user_ids"]
    # Issue slightly in the past: the backend rejects tokens whose iat is in its future.
    issued_at = int(time.time()) - 5
    tokens = [sign(uid, key, args.issuer, issued_at, int(args.ttl_hours * 3600)) for uid in user_ids]

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(tokens))
    os.chmod(args.output, 0o600)
    print(f"Wrote {len(tokens)} tokens to {args.output} (valid for {args.ttl_hours:g} hours)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
