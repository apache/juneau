#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

// Different license headers for different file types
const jsLicenseHeader = `/*
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

`;

const shellLicenseHeader = `#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations under the License.
#

`;

const cssLicenseHeader = `/*
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

`;

// Files that need license headers
const filesToUpdate = [
  'juneau-docs-poc/html-to-md-converter.js',
  'juneau-docs-poc/juneau-documentation/docusaurus.config.ts',
  'juneau-docs-poc/juneau-documentation/start-server.sh',
  'juneau-docs-poc/juneau-documentation/sidebars.ts',
  'juneau-docs-poc/juneau-documentation/sidebars-example.js',
  'juneau-docs-poc/juneau-documentation/flatten-docs.js',
  'juneau-docs-poc/juneau-documentation/process-links.js',
  'juneau-docs-poc/juneau-documentation/src/css/custom.css',
  'juneau-docs-poc/juneau-documentation/src/css/eclipse-syntax.css',
  'juneau-docs-poc/juneau-documentation/src/plugins/remark-juneau-links.ts',
  'juneau-docs-poc/juneau-documentation/src/plugins/juneau-link-processor.ts',
  'juneau-docs-poc/juneau-documentation/src/components/HomepageFeatures/index.tsx',
  'juneau-docs-poc/juneau-documentation/src/components/HomepageFeatures/styles.module.css',
  'juneau-docs-poc/juneau-documentation/src/components/ClassHierarchy.module.css',
  'juneau-docs-poc/juneau-documentation/src/components/ClassHierarchy.tsx',
  'juneau-docs-poc/juneau-documentation/src/pages/index.tsx',
  'juneau-docs-poc/juneau-documentation/src/pages/index.module.css'
];

function getFileExtension(filePath) {
  return path.extname(filePath).toLowerCase();
}

function getLicenseHeader(filePath) {
  const ext = getFileExtension(filePath);
  
  if (filePath.endsWith('.sh')) {
    return shellLicenseHeader;
  } else if (ext === '.css') {
    return cssLicenseHeader;
  } else {
    // Default to JS-style comments for .js, .ts, .tsx files
    return jsLicenseHeader;
  }
}

function hasLicenseHeader(content) {
  return content.includes('Licensed to the Apache Software Foundation');
}

function addLicenseHeader(filePath) {
  const fullPath = path.resolve('/Users/james.bognar/git/juneau', filePath);
  
  if (!fs.existsSync(fullPath)) {
    console.log(`File not found: ${fullPath}`);
    return;
  }
  
  let content = fs.readFileSync(fullPath, 'utf8');
  
  if (hasLicenseHeader(content)) {
    console.log(`License header already exists in: ${filePath}`);
    return;
  }
  
  const licenseHeader = getLicenseHeader(filePath);
  
  // Handle shebang lines
  if (content.startsWith('#!') && !filePath.endsWith('.sh')) {
    const lines = content.split('\n');
    const shebang = lines[0];
    const restOfContent = lines.slice(1).join('\n');
    content = shebang + '\n' + jsLicenseHeader + restOfContent;
  } else if (filePath.endsWith('.sh') && content.startsWith('#!/bin/bash')) {
    // For shell files, replace the shebang with our header that includes shebang
    const lines = content.split('\n');
    const restOfContent = lines.slice(1).join('\n');
    content = licenseHeader + restOfContent;
  } else {
    content = licenseHeader + content;
  }
  
  fs.writeFileSync(fullPath, content, 'utf8');
  console.log(`Added license header to: ${filePath}`);
}

function main() {
  console.log('Adding Apache license headers to source files...\n');
  
  for (const filePath of filesToUpdate) {
    addLicenseHeader(filePath);
  }
  
  console.log('\nLicense header addition complete!');
}

if (require.main === module) {
  main();
}
