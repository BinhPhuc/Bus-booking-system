"""A fake SMTP server that accepts connections and never answers.

Clients wait for the SMTP greeting until their read timeout expires, which is
how a hung mail server looks to the backend.

    python3 smtp_blackhole.py [port]     (default 1025)
"""
import socket
import sys

port = int(sys.argv[1]) if len(sys.argv) > 1 else 1025
server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server.bind(("127.0.0.1", port))
server.listen(1024)
print(f"SMTP blackhole listening on 127.0.0.1:{port}", flush=True)

held = []  # keep sockets referenced so they stay open
while True:
    connection, _ = server.accept()
    held.append(connection)
    if len(held) > 5000:
        held.pop(0).close()
