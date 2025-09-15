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

// Import the component at the top of files that use it
const IMPORT_STATEMENT = `import JuneauVersion from '@site/src/components/JuneauVersion';

`;

function processFile(filePath) {
    try {
        let content = fs.readFileSync(filePath, 'utf8');
        
        // Check if the file contains version numbers
        if (!content.includes('9.0.1')) {
            return false; // No changes needed
        }
        
        let modified = false;
        const originalContent = content;
        
        // Replace version numbers in different contexts
        
        // 1. In Maven dependencies
        content = content.replace(
            /<version>9\.0\.1<\/version>/g, 
            '<version><JuneauVersion /></version>'
        );
        
        // 2. In jar file names  
        content = content.replace(
            /([a-zA-Z-]+)-9\.0\.1\.jar/g,
            '$1-<JuneauVersion />.jar'
        );
        
        // 3. In OSGi module names
        content = content.replace(
            /([a-zA-Z.]+)_9\.0\.1\.jar/g,
            '$1_<JuneauVersion />.jar'
        );
        
        // 4. In plain text mentions (be careful not to replace in URLs or other contexts)
        content = content.replace(
            /(\s|^)9\.0\.1(\s|$|\.)/g,
            '$1<JuneauVersion />$2'
        );
        
        // If we made changes, add the import statement at the top (after frontmatter)
        if (content !== originalContent) {
            // Check if import already exists
            if (!content.includes('import JuneauVersion')) {
                // Find the end of frontmatter
                const frontmatterEnd = content.indexOf('---', 3);
                if (frontmatterEnd !== -1) {
                    // Insert after frontmatter
                    const beforeFrontmatter = content.substring(0, frontmatterEnd + 3);
                    const afterFrontmatter = content.substring(frontmatterEnd + 3);
                    content = beforeFrontmatter + '\n\n' + IMPORT_STATEMENT + afterFrontmatter;
                } else {
                    // No frontmatter, add at the beginning
                    content = IMPORT_STATEMENT + content;
                }
            }
            modified = true;
        }
        
        if (modified) {
            fs.writeFileSync(filePath, content, 'utf8');
            return true;
        }
        
        return false;
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
    
    console.log('Replacing version numbers with <JuneauVersion /> component...\n');
    
    for (const file of files) {
        totalFiles++;
        console.log(`Processing: ${path.basename(file)}`);
        
        if (processFile(file)) {
            console.log(`  ✅ Updated version references`);
            processedCount++;
        } else {
            console.log(`  ⏭️  No version references found`);
        }
    }
    
    console.log(`\n✅ Processed ${totalFiles} files, updated ${processedCount} files with version references.`);
    
    if (processedCount > 0) {
        console.log('\n📝 Usage in Markdown files:');
        console.log('  - Maven dependencies: <version><JuneauVersion /></version>');
        console.log('  - JAR files: juneau-marshall-<JuneauVersion />.jar');
        console.log('  - Text: Version <JuneauVersion />');
        console.log('\n📝 To update the version:');
        console.log('  - Edit docusaurus.config.ts');
        console.log('  - Change customFields.juneauVersion to the new version');
        console.log('  - All references will update automatically!');
    }
}

// Check if glob is available
try {
    require.resolve('glob');
} catch (e) {
    console.error('Error: The "glob" package is required but not installed.');
    console.error('Please run: npm install glob');
    process.exit(1);
}

findAndProcessFiles();
