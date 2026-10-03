#!/usr/bin/env bash
# TEMPORARY (removed before merge): prints Minecraft API signatures the dev container can't download.
set -u
BIN=$(ls build/moddev/artifacts/minecraft-patched-*.jar | grep -v sources | head -1)
jar tf "$BIN" | grep '\.class$' > /tmp/classes.txt
for q in $(grep -v '^#' scripts/ci/probe_find.txt 2>/dev/null); do echo "=================== FIND $q"; grep -iE "$q" /tmp/classes.txt | head -60; done
# Signatures (javap) of every listed class, including nested classes
for n in $(grep -v '^#' scripts/ci/probe_classes.txt); do
  for c in $(grep -E "/$n(\\\$[^/]*)?\.class$" /tmp/classes.txt | grep -v '\$[0-9]' | head -12); do
    cls=${c%.class}; echo "=================== JAVAP ${cls//\//.}"; javap -p -cp "$BIN" "${cls//\//.}" | grep -v 'lambda\$'
  done
done
# Full decompiled source of a few small classes (time-boxed)
DECOMP=$(grep -v '^#' scripts/ci/probe_decompile.txt 2>/dev/null)
if [ -n "$DECOMP" ]; then
  curl -sSLo /tmp/vf.jar https://repo1.maven.org/maven2/org/vineflower/vineflower/1.12.0/vineflower-1.12.0.jar
  mkdir -p /tmp/cls /tmp/out
  SEL=""; for n in $DECOMP; do SEL="$SEL $(grep -E "/$n(\\\$[^/]*)?\.class$" /tmp/classes.txt | tr '\n' ' ')"; done
  (cd /tmp/cls && jar xf "$GITHUB_WORKSPACE/$BIN" $SEL)
  timeout 600 java -jar /tmp/vf.jar -dgs=1 -log=WARN /tmp/cls /tmp/out > /dev/null 2>&1
  for f in $(find /tmp/out -name '*.java'); do echo "=================== SOURCE $f"; grep -v '^import ' "$f"; done
fi
mkdir -p /tmp/res
for j in build/moddev/artifacts/*.jar; do (cd /tmp/res && jar xf "$GITHUB_WORKSPACE/$j" data assets 2>/dev/null); done
for p in $(grep -v '^#' scripts/ci/probe_data.txt 2>/dev/null); do echo "=================== DATA $p"; if [ -d "/tmp/res/$p" ]; then ls "/tmp/res/$p"; else cat "/tmp/res/$p"; fi; echo; done
