#!/bin/bash
# stop-all.sh
# Stops the 4 services started by start-all.sh, using the PID files it wrote.
# Does NOT stop Kafka or Redis - stop those yourself if you want a full teardown.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"

for name in config-server payments auth inventory; do
  pid_file="$LOG_DIR/$name.pid"
  if [ -f "$pid_file" ]; then
    pid=$(cat "$pid_file")
    if kill -0 "$pid" 2>/dev/null; then
      echo "Stopping $name (pid $pid) ..."
      kill "$pid"
    else
      echo "$name (pid $pid) not running"
    fi
    rm -f "$pid_file"
  else
    echo "No pid file for $name, skipping"
  fi
done

echo "Done. (Kafka and Redis are left running - stop them separately if needed.)"
