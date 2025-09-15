#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

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

// Files that were deleted or missing from the original list
const additionalFiles = [
  'juneau-docs-poc/release-notes-converter.js',  // This was deleted
  'juneau-docs-poc/remove-license-headers.js',   // This was deleted  
  'juneau-docs-poc/fix-release-notes-mdx.js'     // This was deleted
];

function addLicenseHeader(filePath) {
  const fullPath = path.resolve('/Users/james.bognar/git/juneau', filePath);
  
  if (!fs.existsSync(fullPath)) {
    console.log(`File not found (likely deleted): ${filePath}`);
    return;
  }
  
  let content = fs.readFileSync(fullPath, 'utf8');
  
  if (content.includes('Licensed to the Apache Software Foundation')) {
    console.log(`License header already exists in: ${filePath}`);
    return;
  }
  
  // Handle shebang lines
  if (content.startsWith('#!')) {
    const lines = content.split('\n');
    const shebang = lines[0];
    const restOfContent = lines.slice(1).join('\n');
    content = shebang + '\n' + jsLicenseHeader + restOfContent;
  } else {
    content = jsLicenseHeader + content;
  }
  
  fs.writeFileSync(fullPath, content, 'utf8');
  console.log(`Added license header to: ${filePath}`);
}

function main() {
  console.log('Checking for any remaining files that need license headers...\n');
  
  for (const filePath of additionalFiles) {
    addLicenseHeader(filePath);
  }
  
  console.log('\nRemaining license header check complete!');
}

if (require.main === module) {
  main();
}
