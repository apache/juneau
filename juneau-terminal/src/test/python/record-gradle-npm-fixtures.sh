#!/usr/bin/env bash
# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
# Records the Gradle and npm parser fixtures.  Usage: record-gradle-npm-fixtures.sh <scratch-dir> <fixtures-dir>
# Needs gradle (8.x or later, JVM 17+) and npm on PATH.  The scratch projects are throwaway; nothing here touches the
# Juneau tree.  --no-daemon, so no Gradle daemon outlives the script.  npm needs no registry: the one dependency is a
# local file: package.
set -euo pipefail
SCRATCH="$1"; OUT="$2"
mkdir -p "$SCRATCH" "$OUT"; SCRATCH="$(cd "$SCRATCH" && pwd -P)"; OUT="$(cd "$OUT" && pwd -P)"

# --- Gradle: two Java projects, :lib (one passing test) and :app (depends on :lib) -----------------------------------
G="$SCRATCH/gradle"; rm -rf "$G"; mkdir -p "$G"; cd "$G"
cat > settings.gradle <<'E'
rootProject.name = 'demo'
include 'lib', 'app'
E
cat > build.gradle <<'E'
subprojects {
  apply plugin: 'java'
  repositories { mavenCentral() }
  dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.3'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
  }
  test { useJUnitPlatform() }
}
project(':app') { dependencies { implementation project(':lib') } }
E
for pc in lib:Lib app:App; do
  p="${pc%%:*}"; c="${pc##*:}"
  mkdir -p "$p/src/main/java/x" "$p/src/test/java/x"
  printf 'package x;\npublic class %s { public static int one() { return 1; } }\n' "$c" > "$p/src/main/java/x/$c.java"
  printf 'package x;\nimport org.junit.jupiter.api.Test;\nimport static org.junit.jupiter.api.Assertions.*;\nclass %sTest {\n  @Test void one() { assertEquals(1, %s.one()); }\n  @Test void two() { assertEquals(1, %s.one()); }\n}\n' "$c" "$c" "$c" > "$p/src/test/java/x/${c}Test.java"
done
gradle --no-daemon --console=plain build > "$OUT/gradle-success.log" 2>&1
# One failing test of two in :app, run again so :lib's tasks are UP-TO-DATE.
printf 'package x;\nimport org.junit.jupiter.api.Test;\nimport static org.junit.jupiter.api.Assertions.*;\nclass AppTest {\n  @Test void one() { assertEquals(1, App.one()); }\n  @Test void two() { assertEquals(2, App.one()); }\n}\n' > app/src/test/java/x/AppTest.java
gradle --no-daemon --console=plain build > "$OUT/gradle-test-failure.log" 2>&1 || true
# A compile error in :lib.
printf 'package x;\npublic class Lib { public static int one() { return "x"; } }\n' > lib/src/main/java/x/Lib.java
gradle --no-daemon --console=plain build > "$OUT/gradle-compile-failure.log" 2>&1 || true

# --- npm: install a local package, then run a script that doesn't exist ---------------------------------------------
N="$SCRATCH/npm"; rm -rf "$N"; mkdir -p "$N/dep"; cd "$N"
printf '{"name":"dep","version":"1.0.0"}\n' > dep/package.json
printf '{"name":"demo","version":"1.0.0","private":true,"dependencies":{"dep":"file:./dep"}}\n' > package.json
npm install --no-audit --no-fund > "$OUT/npm-install-success.log" 2>&1
npm run no-such-script > "$OUT/npm-missing-script.log" 2>&1 || true

# Scrub the scratch path so the fixtures don't carry this machine's directories.
for f in "$OUT"/gradle-*.log "$OUT"/npm-*.log; do
  sed -i.bak "s#$SCRATCH#/scratch#g; s#$HOME#~#g" "$f" && rm -f "$f.bak"
done
