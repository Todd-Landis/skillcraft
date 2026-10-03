#!/usr/bin/env bash
# TEMPORARY (removed before merge): start runServer, wait for startup, report arena/dimension lines, stop.
set -u
mkdir -p run && echo "eula=true" > run/eula.txt
./gradlew runServer --console=plain > /tmp/server.log 2>&1 &
for i in $(seq 1 150); do
  if grep -qE "Arena dimension|Encountered an unexpected exception|crash-reports" /tmp/server.log; then sleep 3; break; fi
  sleep 3
done
echo "===== relevant lines"
grep -E "Arena dimension|skillcraft|SkillCraft|Done \(|Preparing level|dimension" /tmp/server.log | grep -vE "DEBUG|Loading mod|registered" | cut -c1-300 | head -60
echo "===== WARN/ERROR lines"
grep -E "WARN|ERROR" /tmp/server.log | cut -c1-300 | head -60
pkill -f runServer || true; pkill -f java || true
