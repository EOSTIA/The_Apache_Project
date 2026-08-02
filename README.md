# Config Tower

A tiny config-distribution system built to learn how Kafka, Redis, and Spring Boot
microservices actually fit together — not a production system, a learning project.

**What it does:** you push a config key/value to `config-server` (e.g. `payments`,
`jwt.expiry.minutes`, `60`). It saves the plaintext to Redis, encrypts it with
AES-256-GCM, and publishes it on that service's Kafka topic. The target microservice
(payments / auth / inventory) consumes the message, decrypts it, and stores it in a
local in-memory cache — visible instantly on a live dashboard. Each microservice also
demos a circuit breaker on a "flaky downstream call" endpoint.

```
                    ┌──────────────┐
   dashboard  ────► │ config-server│ ──(plaintext)──► Redis (source of truth)
  (push form)        └──────┬───────┘
                             │ AES-GCM encrypt
                             ▼
                 ┌───────────┴────────────┐
                 │   Kafka (KRaft mode)    │
                 │ config.payments         │
                 │ config.auth             │
                 │ config.inventory        │
                 └───┬───────┬────────┬────┘
                     ▼       ▼        ▼
                payments   auth   inventory
                (:8081)  (:8082)   (:8083)
                     │       │        │
              decrypt, cache in ConcurrentHashMap,
              bootstrap from Redis on startup
```

## Project layout

```
config-server/            Spring Boot app: push endpoint, AES-GCM, Kafka producer, Redis
microservices/payments/   Port 8081 - Kafka consumer, decrypt, cache, circuit breaker demo
microservices/auth/       Port 8082 - same pattern, topic config.auth
microservices/inventory/  Port 8083 - same pattern, topic config.inventory
docker/                   Optional docker-compose.yml (Redis) + apache-httpd.conf (TLS/proxy)
dashboard/                Static HTML/CSS/JS dashboard, dark theme, auto-refresh
jmeter/                   3 JMeter test plans
scripts/                  start-all.sh, stop-all.sh, seed-config.sh, test-propagation.sh
```

---

## Prerequisites

- Windows 10/11 with **WSL2** installed (`wsl --install` in an admin PowerShell if you don't have it)
- A WSL Linux distro (Ubuntu recommended) — Kafka and Redis will live here
- **JDK 17+** — install inside WSL (`sudo apt install openjdk-17-jdk`) since that's where you'll run the services. You can also install a JDK on Windows if you'd rather run the jars natively; the instructions below assume WSL for everything except your IDE/VisualSVN.
- **Maven** (`sudo apt install maven`, or use your IDE's bundled Maven)
- **VisualSVN Server** on Windows (for your own SVN repo — see the SVN section near the bottom, this part is entirely manual/your call)

---

## Step 1 — Install Kafka manually in KRaft mode (no ZooKeeper, no Docker)

Do this inside your WSL Ubuntu shell.

```bash
# 1. Get a JDK if you don't have one yet
sudo apt update && sudo apt install -y openjdk-17-jdk

# 2. Download Kafka (adjust version if a newer one exists when you do this)
cd ~
wget https://downloads.apache.org/kafka/3.8.0/kafka_2.13-3.8.0.tgz
tar -xzf kafka_2.13-3.8.0.tgz
mv kafka_2.13-3.8.0 kafka
cd kafka

# 3. Generate a cluster ID and format storage for KRaft mode (only once, ever)
KAFKA_CLUSTER_ID="$(bin/kafka-storage.sh random-uuid)"
bin/kafka-storage.sh format --standalone -t "$KAFKA_CLUSTER_ID" -c config/server.properties

# 4. Start the broker (foreground - open a dedicated terminal tab for this)
bin/kafka-server-start.sh config/server.properties
```

Leave that terminal running. Kafka is now listening on `localhost:9092` — no ZooKeeper
process anywhere, because KRaft mode lets the broker manage its own metadata.

**Sanity check** (new terminal):
```bash
cd ~/kafka
bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```
Should return empty (no error) — topics get created automatically once config-server starts.

---

## Step 2 — Install Redis manually in WSL

```bash
sudo apt update
sudo apt install -y redis-server

# Start it in the background
redis-server --daemonize yes

# Sanity check
redis-cli ping   # should print PONG
```

Redis is now listening on `localhost:6379`.

---

## Step 3 — Build the 4 Spring Boot apps

Each folder (`config-server`, `microservices/payments`, `microservices/auth`,
`microservices/inventory`) is an independent Maven project — there's no shared parent
module, on purpose, to keep the build simple. Build each one:

```bash
cd config-server && mvn clean package -DskipTests && cd ..
cd microservices/payments && mvn clean package -DskipTests && cd ../..
cd microservices/auth && mvn clean package -DskipTests && cd ../..
cd microservices/inventory && mvn clean package -DskipTests && cd ../..
```

This downloads Spring Boot, Spring Kafka, Spring Data Redis, and Resilience4j from
Maven Central the first time — needs internet access.

---

## Step 4 — Run everything

With Kafka and Redis already running (steps 1–2), from the project root:

```bash
chmod +x scripts/*.sh   # first time only
./scripts/start-all.sh
```

This starts config-server first (so it creates the 3 Kafka topics), waits 3 seconds,
then starts payments, auth, and inventory. Logs go to `logs/<service>.log`.
If you're using Windows to open the dashboard or run `curl.exe`, use the WSL IP printed
by `scripts/start-all.sh` when localhost forwarding is unavailable.

Check health:
```bash
curl http://localhost:8084/api/config/health
curl http://localhost:8081/api/payments/health
curl http://localhost:8082/api/auth/health
curl http://localhost:8083/api/inventory/health
```

Stop everything:
```bash
./scripts/stop-all.sh
```

---

## Step 5 — Seed some dummy config and open the dashboard

```bash
./scripts/seed-config.sh
```

This pushes 18 realistic config values (6 per service) through config-server.

Then just open `dashboard/index.html` directly in your browser (double-click it, or
`file://` path) — it's a static file, no server needed. It polls all three services
every 3 seconds and shows their live config state, plus a push form and activity log.
When you run `./scripts/start-all.sh`, it also writes `dashboard/runtime-config.js` with
the current WSL IP so the dashboard can reach the services even when Windows localhost
forwarding is unavailable.

> If your browser blocks `fetch()` from a `file://` page for CORS reasons, serve the
> dashboard folder with a trivial local server instead:
> `cd dashboard && python3 -m http.server 5500` then open `http://localhost:5500`.

---

## Step 6 — Try the circuit breaker demo

```bash
# Hammer the payments "flaky downstream" endpoint a bunch of times
for i in {1..20}; do curl -s -X POST "http://localhost:8081/api/payments/process?amount=50"; echo; sleep 0.3; done
```

You'll see a mix of successes and fallback messages. If ~50%+ of a rolling window of
10 calls fail, the breaker **opens** and every call short-circuits straight to the
fallback (no real call attempted) for 8 seconds, then goes **half-open** to test if the
downstream has recovered. Same pattern exists on `/api/auth/verify-token` and
`/api/inventory/check-stock`.

---

## Step 7 — Measure propagation latency

```bash
./scripts/test-propagation.sh payments 8081
```

Pushes one value and times exactly how long it takes to become visible on the target
service's REST endpoint — the real push → Redis → encrypt → Kafka → decrypt → cache
round trip, in milliseconds.

---

## Step 8 (optional) — Apache reverse proxy with TLS

`docker/apache-httpd.conf` reverse-proxies all 4 services behind a single HTTPS
endpoint with security headers and basic rate limiting. This is optional — the
dashboard and curl examples above talk to each service's port directly. See the
comments at the top of that file for setup steps (self-signed cert generation, module
requirements, where to `Include` it).

## Optional — Docker Redis instead of WSL Redis

If WSL Redis is giving you trouble, `docker/docker-compose.yml` spins up Redis in a
container instead: `docker compose -f docker/docker-compose.yml up -d`. Pick one path
(WSL or Docker) for Redis, not both — the WSL path is what the rest of this README and
`INTERVIEW_GUIDE.md` assume you can explain in depth.

---

## Load testing with JMeter

See `jmeter/README.md` — three test plans (push throughput, Redis read throughput,
propagation under concurrent load).

---

## Putting this under SVN (VisualSVN Server)

This part is entirely manual — the project intentionally does **not** script your SVN
setup, since that's environment-specific to your machine and you should understand
each step yourself:

1. Install VisualSVN Server on Windows, create a new repository (e.g. `config-tower`) through its management console.
2. On your dev machine, install TortoiseSVN (or use your IDE's SVN integration).
3. Right-click the `config-tower` project folder → **TortoiseSVN → Import** → point it at your VisualSVN repository URL → commit.
4. From then on: **SVN Commit** after changes, **SVN Update** before starting work, branch/tag via **TortoiseSVN → Branch/Tag** if you want to show that on your resume too.
5. Add a `.gitignore`-style exclude list in TortoiseSVN settings for `target/`, `logs/`, and `*.log` so build output doesn't get committed.

---


