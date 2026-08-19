/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { defineConfig, devices } from '@playwright/test';
import path from 'node:path';
import os from 'node:os';

/**
 * Dedicated test port for the Spring Boot app this suite boots for itself. MUST NOT be 8790 — that port is
 * owned by a separately-running coordinator instance of the app that this suite must never touch.
 */
const TEST_PORT = 8791;
const BASE_URL = `http://127.0.0.1:${TEST_PORT}`;

// The app repo root is the parent of this e2e/ directory.
const APP_ROOT = path.resolve(__dirname, '..');

// Personal JDK layout used across this workspace: ~/jdk/default -> current JDK 17 install.
const JAVA_HOME = process.env.JAVA_HOME ?? path.join(os.homedir(), 'jdk', 'default');

export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  // Single worker always: all tests share ONE webServer-booted app instance (dedicated test port), and hammering
  // a single dev-mode Tomcat/Spring instance with concurrent first-touch requests was observed to intermittently
  // 500 (likely a lazy-init race in the app, not something this suite should paper over with retries). Re-tested
  // with the unified-paging-ribbon suite (Aug 2026): re-enabling parallelism (8 workers) still failed ~2/3 runs
  // on the very first navigation (`/rest/releases` intermittently 500s before the app's lazy init settles).
  workers: 1,
  reporter: 'html',
  timeout: 30_000,

  use: {
    baseURL: BASE_URL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // Boots the app on its OWN dedicated port (8791), fully independent of the coordinator's live instance on
  // 8790. `spring-boot.run.arguments=--server.port=...` passes a command-line argument to the forked app JVM,
  // which Spring's property-source precedence honors over the server.port=8790 baked into
  // application.properties. Maven dependency resolution can be slow on a cold cache, but the app itself starts
  // in ~1s once the classpath is resolved — 120s covers both. `reuseExistingServer: !CI` lets a developer who
  // already has a local instance running on 8791 skip the (re)boot; it never touches 8790 either way.
  webServer: {
    command:
      `mvn -q spring-boot:run -Dspring-boot.run.arguments=--server.port=${TEST_PORT}`,
    url: BASE_URL + '/rest/home',
    cwd: APP_ROOT,
    timeout: 120_000,
    reuseExistingServer: !process.env.CI,
    env: {
      ...process.env,
      JAVA_HOME,
      PATH: `${path.join(JAVA_HOME, 'bin')}:${process.env.PATH}`,
    },
  },
});
