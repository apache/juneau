/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

import type {ReactNode} from 'react';
import Link from '@docusaurus/Link';
import {GROUPS, PROJECTS} from '@site/src/data/projects';
import {defineConfig} from '@playwright/test';

// Serves the existing build/ (nothing is built here). reuseExistingServer is false so a stray process
// on the port can never make the tests pass against the wrong site.
export default defineConfig({
  testDir: 'tests/smoke',
  use: {baseURL: 'http://localhost:3111'},
  webServer: {
    command: 'npm run serve -- --port 3111 --no-open',
    url: 'http://localhost:3111/about/',
    reuseExistingServer: false,
  },
});
