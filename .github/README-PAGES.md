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

### GitHub Pages Setup
After pushing this workflow, enable GitHub Pages in your repository:

1. Go to **Settings** → **Pages**
2. Under **Source**, select **GitHub Actions** (not "Deploy from a branch")
3. **Important**: Make sure the repository has Pages enabled in the repository settings
4. If you see "Get Pages site failed" error, verify:
   - Repository is public OR you have GitHub Pro/Enterprise for private repo Pages
   - Pages feature is enabled for the repository
   - GitHub Actions has permission to deploy Pages
5. The site will be available at: `https://<username>.github.io/juneau/`

### Troubleshooting GitHub Pages Deployment

If you encounter "Get Pages site failed" error:

1. **Check Repository Settings**:
   - Go to **Settings** → **Pages**
   - Ensure **Source** is set to "GitHub Actions"
   - Verify the repository has Pages enabled

2. **Check Permissions**:
   - Go to **Settings** → **Actions** → **General**
   - Under "Workflow permissions", ensure "Read and write permissions" is selected
   - Check that "Allow GitHub Actions to create and approve pull requests" is enabled

3. **Repository Visibility**:
   - Public repositories: Pages is free
   - Private repositories: Requires GitHub Pro, Team, or Enterprise

4. **Manual Enablement**:
   - If the workflow fails, you may need to manually enable Pages first
   - Create a simple `index.html` in a `gh-pages` branch, then switch back to Actions

5. **Apache-Specific Requirements**:
   - Apache projects require `.asf.yaml` configuration for GitHub Pages
   - The `ghpages: whoami: asf-site` configuration has been added to enable Pages
   - See: https://cwiki.apache.org/confluence/pages/viewpage.action?pageId=127405038

### Monitoring Deployments
To monitor deployment status and receive notifications:

1. **GitHub Pages Badge** - The README includes a status badge showing deployment status
2. **GitHub Notifications** - Enable repository notifications:
   - Go to the repository → **Watch** → **Custom** → **Actions**
   - Enable notifications for workflow runs in your GitHub profile settings
3. **Actions Tab** - Monitor deployments directly in the repository's Actions tab
4. **Commit Status** - Deployment status appears on commits and pull requests

### Deployment Status
- ✅ **Success**: Site is deployed and accessible at the GitHub Pages URL
- ❌ **Failure**: Check the Actions tab for detailed error logs
- 🟡 **In Progress**: Deployment is currently running

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
