#!/usr/bin/env bash
# Client smoke test (CI job "Client smoke", also works on Linux with Xvfb installed):
#   bash scripts/ci/client_smoke.sh world    dedicated server creates a flat world, copied to run/saves/showcase
#   bash scripts/ci/client_smoke.sh client   client loads it under Xvfb, runs the showcase (dev/Showcase), quits
#   bash scripts/ci/client_smoke.sh check    fails on a crash, a missing "[showcase] done" or missing screenshots,
#                                            and lists SkillCraft / model / texture / animation problems from the log
set -u
step=${1:?usage: client_smoke.sh world|client|check}
mkdir -p run

case "$step" in
world)
  echo "eula=true" > run/eula.txt
  cat > run/server.properties <<'EOF'
level-name=world
level-type=minecraft\:flat
generate-structures=false
online-mode=false
spawn-protection=0
EOF
  rm -rf run/world
  timeout 900 ./gradlew runServer -Pshowcase --console=plain > server.log 2>&1
  grep -E "\[showcase\]|Done \(" server.log | cut -c1-200
  if [ ! -f run/world/level.dat ]; then
    echo "::error::The dedicated server did not create run/world"
    grep -E "ERROR|Exception" server.log | head -40
    exit 1
  fi
  rm -rf run/saves/showcase && mkdir -p run/saves && cp -r run/world run/saves/showcase
  ;;
client)
  cat > run/options.txt <<'EOF'
onboardAccessibility:false
pauseOnLostFocus:false
renderDistance:6
simulationDistance:5
tutorialStep:none
joinedFirstServer:true
skipMultiplayerWarning:true
soundCategory_master:0.0
guiScale:2
EOF
  rm -rf run/screenshots run/crash-reports
  # Mesa software rendering: slow but enough to load the game and draw the scene
  export LIBGL_ALWAYS_SOFTWARE=1
  timeout 1500 xvfb-run -a -s "-screen 0 1280x720x24" ./gradlew runClient -Pshowcase -Pworld=showcase --console=plain > client.log 2>&1
  echo "client exited with $?"
  ;;
check)
  status=0
  grep -E "\[showcase\]" client.log | cut -c1-200
  echo "===== OpenGL / Vulkan"
  grep -iE "OpenGL|GL_RENDERER|Vulkan|Backend library|llvmpipe" client.log | head -8 | cut -c1-200
  echo "===== Possible problems (SkillCraft, models, textures, animations, exceptions)"
  grep -E "WARN|ERROR" client.log \
    | grep -iE "skillcraft|missing|unable to|failed to load|couldn't|exception|animation" \
    | grep -viE "OpenAL|sound|narrator|realms|telemetry|authlib|Yggdrasil|profile|joystick|gamepad" \
    | cut -c1-300 | sort | uniq -c | sort -rn | head -60
  if ls run/crash-reports/*.txt >/dev/null 2>&1; then
    echo "::error::The client crashed"
    head -80 run/crash-reports/*.txt
    status=1
  fi
  if ! grep -q "\[showcase\] done" client.log; then
    echo "::error::The showcase did not finish (see client.log)"
    tail -60 client.log | cut -c1-300
    status=1
  fi
  count=$(ls run/screenshots/showcase_*.png 2>/dev/null | wc -l)
  echo "screenshots: $count"
  if [ "$count" -lt 10 ]; then
    echo "::error::Expected at least 10 showcase screenshots, found $count"
    status=1
  fi
  exit $status
  ;;
esac
