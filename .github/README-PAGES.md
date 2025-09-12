<!--
 ***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
 * with the License.  You may obtain a copy of the License at                                                              *
 *                                                                                                                         *
 *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
 *                                                                                                                         *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
 * specific language governing permissions and limitations under the License.                                              *
 ***************************************************************************************************************************
-->
# GitHub Pages Configuration for Juneau

## Overview

This repository is configured to automatically deploy documentation to GitHub Pages from a combination of:
- Static documentation files in `/docs`
- Generated Javadocs from `mvn javadoc:aggregate`

## How it works

### Automatic Deployment
The `pages.yml` workflow automatically deploys to GitHub Pages when:
- Code is pushed to the `master` or `main` branch
- Changes are made to `docs/**`, `juneau-*/**`, `pom.xml`, or the workflow file
- The workflow is manually triggered from the Actions tab

### Build Process
1. **Environment Setup**: Sets up Java 17 and Maven with dependency caching
2. **Build**: Compiles Juneau modules with `mvn clean compile -DskipTests`
3. **Site Generation**:
   - Builds the `juneau-doc` module (for custom documentation)
   - Generates complete project site with `mvn site -DskipTests`
   - Includes: Javadocs, test reports, dependency info, project reports
4. **Deployment**: Uploads entire `target/site` folder to GitHub Pages

### Documentation Structure
Once deployed, the comprehensive site will be available at:
- **Project Home**: `https://<username>.github.io/juneau/`
- **API Documentation**: `https://<username>.github.io/juneau/apidocs/` (if generated)
- **Test Reports**: `https://<username>.github.io/juneau/surefire.html`
- **Dependencies**: `https://<username>.github.io/juneau/dependencies.html`
- **Project Reports**: `https://<username>.github.io/juneau/project-reports.html`

## Manual Setup Required

After pushing this workflow, enable GitHub Pages in your repository:

1. Go to **Settings** → **Pages**
2. Under **Source**, select **GitHub Actions**
3. The site will be available at: `https://<username>.github.io/juneau/`

## Local Development

To generate the complete project site locally:

```bash
# Build the project
mvn clean compile -DskipTests

# Build documentation module
cd juneau-doc
mvn install -DskipTests
cd ..

# Generate complete project site
mvn site -DskipTests
```

The generated site will be available in:
- Complete site: `target/site/index.html`
- Project reports: `target/site/project-reports.html`
- Dependencies: `target/site/dependencies.html`
- Test results: `target/site/surefire.html`
- Javadocs: `target/site/apidocs/` (if aggregate goal works)

## Integration with Existing Build Scripts

This GitHub Pages workflow is designed to work alongside the existing `juneau-build-javadoc.sh` script:
- The GitHub workflow focuses on public documentation deployment
- The existing script handles more complex documentation generation and website publishing
- Both can coexist without conflicts
