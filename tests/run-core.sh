#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
java_bin="${JAVA_HOME:+$JAVA_HOME/bin/}java"
javac_bin="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
if [ -n "${GSON_JAR:-}" ]; then
  gson="$GSON_JAR"
else
  gson="$work/gson.jar"
  curl -fsSL --retry 2 https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar -o "$gson"
fi
printf '527175ca6d81050b53bdd4c457a6d6e017626b0e  %s\n' "$gson" | sha1sum -c -
"$javac_bin" -encoding UTF-8 --release 21 -cp "$gson" -d "$work/classes" \
  src/main/java/com/visionmod/{LrcTimeline,LyricsSearch,SystemMediaTrack,SystemMediaReader,BoundedLyricsBody,DashboardLayout,PanelLayout,EffectMath,HudPacking,HotbarTween}.java tests/CoreRegressionTest.java
"$java_bin" -ea -cp "$work/classes:$gson" CoreRegressionTest "$@"

"$javac_bin" -encoding UTF-8 --release 21 -cp "$gson:$work/classes" -d "$work/classes" \
  tests/stubs/net/minecraft/client/Minecraft.java tests/stubs/net/fabricmc/loader/api/FabricLoader.java \
  src/main/java/com/visionmod/{VisionLyricsSource,VisionLyricsLookup}.java tests/LyricsSourceTest.java tests/LyricsLookupRegressionTest.java
"$java_bin" -ea -Dvisionmod.test.config="$work/config" -cp "$work/classes:$gson" LyricsSourceTest
"$java_bin" -ea -cp "$work/classes:$gson" com.visionmod.LyricsLookupRegressionTest

"$javac_bin" -encoding UTF-8 --release 21 -cp "$work/classes" -d "$work/classes" \
  tests/stubs/com/mojang/blaze3d/vertex/*.java tests/stubs/net/minecraft/resources/Identifier.java \
  tests/stubs/net/minecraft/client/renderer/rendertype/RenderTypes.java \
  tests/stubs/net/minecraft/client/renderer/texture/OverlayTexture.java \
  src/main/java/com/visionmod/VisionGeometry.java tests/MeshRegressionTest.java
"$java_bin" -ea -cp "$work/classes" MeshRegressionTest
