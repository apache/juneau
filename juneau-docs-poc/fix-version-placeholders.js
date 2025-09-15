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
const glob = require('glob');

function processFile(filePath) {
    try {
        let content = fs.readFileSync(filePath, 'utf8');
        
        // Check if the file contains JuneauVersion components
        if (!content.includes('<JuneauVersion />')) {
            return false;
        }
        
        let modified = false;
        const originalContent = content;
        
        // Replace <JuneauVersion /> with {{JUNEAU_VERSION}}
        content = content.replace(/<JuneauVersion \/>/g, '{{JUNEAU_VERSION}}');
        
        // Remove the import statement if it exists
        content = content.replace(/import JuneauVersion from '@site\/src\/components\/JuneauVersion';\s*\n/g, '');
        
        // Remove extra blank lines that might be left
        content = content.replace(/\n\n\n+/g, '\n\n');
        
        if (content !== originalContent) {
            fs.writeFileSync(filePath, content, 'utf8');
            modified = true;
        }
        
        return modified;
    } catch (error) {
        console.error(`Error processing ${filePath}:`, error.message);
        return false;
    }
}

function findAndProcessFiles() {
    const docsPattern = '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/**/*.md';
    
    // Find all markdown files
    const files = glob.sync(docsPattern);
    
    let processedCount = 0;
    let totalFiles = 0;
    
    console.log('Replacing <JuneauVersion /> with {{JUNEAU_VERSION}} placeholders...\n');
    
    for (const file of files) {
        totalFiles++;
        console.log(`Processing: ${path.basename(file)}`);
        
        if (processFile(file)) {
            console.log(`  ✅ Updated version references`);
            processedCount++;
        } else {
            console.log(`  ⏭️  No JuneauVersion components found`);
        }
    }
    
    console.log(`\n✅ Processed ${totalFiles} files, updated ${processedCount} files.`);
    
    if (processedCount > 0) {
        console.log('\n📝 Now using {{JUNEAU_VERSION}} placeholders that work in code blocks!');
        console.log('\n📝 To update the version:');
        console.log('  - Edit docusaurus.config.ts');
        console.log('  - Change the version in remarkVersionReplacer options');
        console.log('  - All {{JUNEAU_VERSION}} placeholders will be replaced automatically!');
    }
}

findAndProcessFiles();
