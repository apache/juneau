#!/usr/bin/env node
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


const fs = require('fs');
const path = require('path');

/**
 * Script to flatten the hierarchical documentation structure into a sequential numbering system
 * 
 * Examples:
 * 01.Overview.html -> 01.01.Overview.html
 * 01.Overview/01.o.Marshalling.html -> 01.02.Marshalling.html
 * 01.Overview/02.o.EndToEndRest.html -> 01.03.EndToEndRest.html
 * 02.juneau-marshall/04.jm.JavaBeansSupport/01.jm.BeanAnnotation.html -> 02.05.02.BeanAnnotation.html
 */

const sourceDir = './old_docs';
const targetDir = './flattened_docs';

// Create target directory
if (!fs.existsSync(targetDir)) {
    fs.mkdirSync(targetDir, { recursive: true });
}

// Counter for sequential numbering within each major section
const sectionCounters = {};

function processDirectory(dirPath, sectionPrefix = '') {
    const items = fs.readdirSync(dirPath).sort();
    
    for (const item of items) {
        const itemPath = path.join(dirPath, item);
        const stat = fs.statSync(itemPath);
        
        if (item === 'doc-files') {
            // Skip doc-files directories for now
            continue;
        }
        
        if (stat.isFile() && item.endsWith('.html')) {
            processHtmlFile(itemPath, item, sectionPrefix);
        } else if (stat.isDirectory()) {
            // Extract section number from directory name
            const match = item.match(/^(\d+)\.(.+)/);
            if (match) {
                const [, sectionNum, sectionName] = match;
                const newSectionPrefix = sectionPrefix ? `${sectionPrefix}.${sectionNum}` : sectionNum;
                processDirectory(itemPath, newSectionPrefix);
            }
        }
    }
}

function processHtmlFile(filePath, fileName, sectionPrefix) {
    // Extract the base name and clean it up
    const match = fileName.match(/^(\d+)\.(.+)\.html$/);
    if (!match) return;
    
    const [, fileNum, rawName] = match;
    
    // Clean up the name by removing prefixes like "o.", "jm.", etc.
    let cleanName = rawName
        .replace(/^[a-z]+\./, '') // Remove prefixes like "o.", "jm.", "jd.", etc.
        .replace(/([A-Z])/g, '$1') // Keep camelCase as is
        .replace(/^./, c => c.toUpperCase()); // Capitalize first letter
    
    // Generate the new sequential number
    if (!sectionCounters[sectionPrefix]) {
        sectionCounters[sectionPrefix] = 1;
    } else {
        sectionCounters[sectionPrefix]++;
    }
    
    const newNumber = sectionCounters[sectionPrefix].toString().padStart(2, '0');
    const newFileName = `${sectionPrefix}.${newNumber}.${cleanName}.html`;
    
    console.log(`${filePath} -> ${newFileName}`);
    
    // Copy the file with the new name
    const targetPath = path.join(targetDir, newFileName);
    fs.copyFileSync(filePath, targetPath);
}

// Special handling for root-level files
function processRootFiles() {
    const rootFiles = fs.readdirSync(sourceDir).filter(item => 
        item.endsWith('.html') && fs.statSync(path.join(sourceDir, item)).isFile()
    );
    
    rootFiles.sort().forEach((file, index) => {
        const match = file.match(/^(\d+)\.(.+)\.html$/);
        if (match) {
            const [, sectionNum, rawName] = match;
            
            // Initialize counter for this section
            sectionCounters[sectionNum] = 1;
            
            // Clean up the name
            let cleanName = rawName
                .replace(/([A-Z])/g, '$1')
                .replace(/^./, c => c.toUpperCase());
            
            const newFileName = `${sectionNum}.01.${cleanName}.html`;
            console.log(`${file} -> ${newFileName}`);
            
            const sourcePath = path.join(sourceDir, file);
            const targetPath = path.join(targetDir, newFileName);
            fs.copyFileSync(sourcePath, targetPath);
        }
    });
}

// Main execution
console.log('🔄 Flattening documentation structure...');
console.log(`📁 Source: ${sourceDir}`);
console.log(`📁 Target: ${targetDir}`);
console.log('');

// Process root files first
processRootFiles();

// Then process directories
const directories = fs.readdirSync(sourceDir).filter(item => 
    fs.statSync(path.join(sourceDir, item)).isDirectory()
);

directories.sort().forEach(dir => {
    const match = dir.match(/^(\d+)\.(.+)/);
    if (match) {
        const [, sectionNum] = match;
        processDirectory(path.join(sourceDir, dir), sectionNum);
    }
});

console.log('');
console.log('✅ Flattening complete!');
console.log(`📊 Generated ${Object.values(sectionCounters).reduce((a, b) => a + b, 0)} files`);
