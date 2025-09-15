#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

const correctLicenseHeader = `<!--
 Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  
 See the NOTICE file distributed with this work for additional information regarding copyright ownership.  
 The ASF licenses this file to you under the Apache License, Version 2.0 (the "License"); 
 you may not use this file except in compliance with the License.  
 You may obtain a copy of the License at
  
  http://www.apache.org/licenses/LICENSE-2.0
  
 Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
 See the License for the specific language governing permissions and limitations under the License.
-->`;

function fixLicenseInFile(filePath) {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        
        // Check if the file has the problematic license header
        if (content.includes('Licensed to the Apache Software Foundation')) {
            // Remove the old license header (everything before the first ---)
            const frontmatterStart = content.indexOf('---');
            if (frontmatterStart > 0) {
                const contentAfterLicense = content.substring(frontmatterStart);
                const newContent = correctLicenseHeader + '\n\n' + contentAfterLicense;
                
                fs.writeFileSync(filePath, newContent, 'utf8');
                console.log(`Fixed license header in ${filePath}`);
            } else {
                console.log(`No frontmatter found in ${filePath}, skipping`);
            }
        } else {
            console.log(`No license header found in ${filePath}, skipping`);
        }
    } catch (error) {
        console.error(`Error processing ${filePath}:`, error.message);
    }
}

function processDirectory(dirPath) {
    const files = fs.readdirSync(dirPath);
    
    for (const file of files) {
        const fullPath = path.join(dirPath, file);
        const stat = fs.statSync(fullPath);
        
        if (stat.isDirectory()) {
            processDirectory(fullPath);
        } else if (file.endsWith('.md')) {
            fixLicenseInFile(fullPath);
        }
    }
}

// Process the topics directory
const topicsDir = path.join(__dirname, 'juneau-documentation', 'docs', 'topics');
console.log(`Processing directory: ${topicsDir}`);
processDirectory(topicsDir);

console.log('License header fix complete!');
