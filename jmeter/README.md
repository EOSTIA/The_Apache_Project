# JMeter Test Plans

Three small test plans. All assume config-server (8084), payments (8081), auth (8082), and inventory (8083) are already running, and Kafka + Redis are up.

Download JMeter from https://jmeter.apache.org/download_jmeter.cgi (no install needed, it's a zip - just extract and run `bin\jmeter.bat` on Windows).

## 1. push-throughput.jmx
Fires 500 total POST `/api/config/push` requests (10 threads × 50 loops) at config-server and reports requests/sec, average latency, and error rate for the encrypt-and-publish path.

## 2. redis-reads.jmx
20 threads × 100 loops, each hitting all three `GET /api/config/redis/{service}` endpoints — measures how config-server's Redis-backed reads hold up under concurrent load.

## 3. propagation-under-load.jmx
Two thread groups running at the same time:
- **Background Push Load**: 8 threads continuously pushing "noise" config values, to simulate a busy system.
- **Poller**: 2 threads polling payments' `/api/payments/config` every 500ms.

Watch the Summary Report while it runs — you're checking that GETs on the poller thread stay fast and error-free even while the system is under write load. This is the qualitative "does propagation hold up" test; use `scripts/test-propagation.sh` for a precise single-value latency number.

## How to run
```
bin\jmeter.bat -t push-throughput.jmx
```
Or open the GUI (`bin\jmeter.bat`) and load the .jmx file via File > Open, then hit the green Start arrow. Right-click the "Summary Report" element to view results as they stream in.

## What to actually say about this in an interview
Be honest: this is JMeter, not a production load-testing setup. What it demonstrates is that you know how to define concurrent virtual users, ramp-up periods, and read a summary report for throughput/latency/error-rate — the basics of load testing a REST API. Don't oversell it as "stress-tested to production scale."
