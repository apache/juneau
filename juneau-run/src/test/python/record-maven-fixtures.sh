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
# Records the scratch-project Maven fixtures.  Usage: record-maven-fixtures.sh <scratch-dir> <fixtures-dir> [extra mvn args...]
# The scratch project is a throwaway 3-module reactor; nothing here touches the Juneau tree.
set -euo pipefail
SCRATCH="$1"; OUT="$2"; shift 2
mkdir -p "$SCRATCH" "$OUT"; SCRATCH="$(cd "$SCRATCH" && pwd -P)"; OUT="$(cd "$OUT" && pwd -P)"
cd "$SCRATCH"
cat > pom.xml <<'E'
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>demo</groupId><artifactId>demo-parent</artifactId><version>1.0</version><packaging>pom</packaging>
  <name>Demo Parent</name>
  <modules><module>mod-a</module><module>mod-b</module><module>mod-c</module></modules>
  <properties><maven.compiler.release>17</maven.compiler.release><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
  <dependencyManagement><dependencies>
    <dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId><version>5.11.3</version><scope>test</scope></dependency>
  </dependencies></dependencyManagement>
  <build><pluginManagement><plugins>
    <plugin><artifactId>maven-compiler-plugin</artifactId><version>3.14.0</version></plugin>
    <plugin><artifactId>maven-resources-plugin</artifactId><version>3.3.1</version></plugin>
    <plugin><artifactId>maven-surefire-plugin</artifactId><version>3.5.4</version></plugin>
  </plugins></pluginManagement></build>
</project>
E
for m in a b c; do
  mkdir -p "mod-$m/src/main/java/x" "mod-$m/src/test/java/x"
  cat > "mod-$m/pom.xml" <<E
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <parent><groupId>demo</groupId><artifactId>demo-parent</artifactId><version>1.0</version></parent>
  <artifactId>mod-$m</artifactId><name>Demo Module $(echo $m | tr a-c A-C)</name>
  <dependencies><dependency><groupId>org.junit.jupiter</groupId><artifactId>junit-jupiter</artifactId></dependency></dependencies>
</project>
E
  echo 'package x;
public class Calc { public int add(int a, int b) { return a + b; } }' > "mod-$m/src/main/java/x/Calc.java"
done
cat > mod-a/src/test/java/x/CalcTest.java <<'E'
package x;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class CalcTest {
  @Test void adds() { assertEquals(3, new Calc().add(1, 2)); }
  @Test void addsNegative() { assertEquals(-1, new Calc().add(1, -2)); }
}
E
cat > mod-c/src/test/java/x/CalcTest.java <<'E'
package x;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class CalcTest { @Test void adds() { assertEquals(3, new Calc().add(1, 2)); } }
E
write_b() {  # $1 = expected value in brokenSum
cat > mod-b/src/test/java/x/CalcTest.java <<E
package x;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
class CalcTest {
  @Test void adds() { assertEquals(3, new Calc().add(1, 2)); }
  @Test void brokenSum() { assertEquals($1, new Calc().add(1, 2)); }
  @Disabled @Test void later() { }
}
E
}
# Replace the scratch path with a stable one so fixtures carry no local paths.
clean() { sed -e "s#$SCRATCH#/work/demo#g" -e "s#$HOME#/home/user#g" > "$OUT/$1"; }
rec() { local name="$1"; shift; (mvn -B "$@" > "$SCRATCH/out.raw" 2>&1 || true); clean "$name" < "$SCRATCH/out.raw"; }
write_b 5
rec maven-test-failure.log "$@" test
echo 'class Bad { int x = "s"; }' > mod-b/src/main/java/x/Bad.java
rec maven-compile-failure.log "$@" test
rm mod-b/src/main/java/x/Bad.java
rec maven-quiet-failure.log "$@" -q test
write_b 3
rec maven-serial-success.log "$@" test
rec maven-parallel-success.log "$@" -T2 test
echo "recorded into $OUT"
