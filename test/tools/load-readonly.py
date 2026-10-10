"""Bounded localhost-only response-time sample; never a worldwide capacity claim."""

import argparse, concurrent.futures, json, statistics, time, urllib.request, urllib.parse

parser = argparse.ArgumentParser()
parser.add_argument("--base", default="http://127.0.0.1:8089")
parser.add_argument("--requests", type=int, default=60)
parser.add_argument("--concurrency", type=int, default=5)
args = parser.parse_args()
uri = urllib.parse.urlparse(args.base)
if uri.hostname not in ("localhost", "127.0.0.1") or uri.scheme != "http":
    raise SystemExit("Only isolated localhost HTTP targets are allowed.")
if not 1 <= args.requests <= 200 or not 1 <= args.concurrency <= 10:
    raise SystemExit("Maximum 200 requests and concurrency 10.")


def call(i):
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(
            args.base + "/api/v1/equipment?page=0&size=12", timeout=20
        ) as response:
            response.read()
            return (time.perf_counter() - start) * 1000, response.status
    except Exception:
        return (time.perf_counter() - start) * 1000, 0


start = time.perf_counter()
with concurrent.futures.ThreadPoolExecutor(max_workers=args.concurrency) as pool:
    rows = list(pool.map(call, range(args.requests)))
times = sorted(row[0] for row in rows)
print(
    json.dumps(
        {
            "scope": "isolated localhost API only",
            "requests": len(rows),
            "concurrency": args.concurrency,
            "errors": sum(code != 200 for _, code in rows),
            "p50_ms": round(statistics.median(times), 2),
            "p95_ms": round(times[min(len(times) - 1, int(len(times) * 0.95))], 2),
            "elapsed_seconds": round(time.perf_counter() - start, 2),
        },
        indent=2,
    )
)
