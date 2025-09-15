#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

const licenseHeader = `<!--
/***************************************************************************************************************************
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
 ***************************************************************************************************************************/
 -->
`;

function addLicenseToFile(filePath) {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        
        // Check if license header already exists
        if (content.includes('Licensed to the Apache Software Foundation')) {
            console.log(`Skipping ${filePath} - license header already exists`);
            return;
        }
        
        // Add license header at the beginning
        const newContent = licenseHeader + '\n' + content;
        
        fs.writeFileSync(filePath, newContent, 'utf8');
        console.log(`Added license header to ${filePath}`);
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
            addLicenseToFile(fullPath);
        }
    }
}

// Process the topics directory
const topicsDir = path.join(__dirname, 'juneau-documentation', 'docs', 'topics');
console.log(`Processing directory: ${topicsDir}`);
processDirectory(topicsDir);

console.log('License header addition complete!');
