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

function improveMarkdownFormatting(content) {
    let lines = content.split('\n');
    let improved = [];
    
    for (let i = 0; i < lines.length; i++) {
        const currentLine = lines[i];
        const prevLine = i > 0 ? lines[i - 1] : '';
        const nextLine = i < lines.length - 1 ? lines[i + 1] : '';
        
        // Add the current line
        improved.push(currentLine);
        
        // Add blank lines after specific patterns for better readability
        
        // After headers (but not if next line is already blank)
        if (currentLine.match(/^#{1,6}\s+/) && nextLine.trim() !== '' && !nextLine.match(/^#{1,6}\s+/)) {
            improved.push('');
        }
        
        // After code blocks end
        if (currentLine.trim() === '```' && prevLine.trim() !== '' && nextLine.trim() !== '') {
            improved.push('');
        }
        
        // Before code blocks start (but not if previous line is already blank)
        if (currentLine.match(/^```\w*$/) && prevLine.trim() !== '' && !prevLine.match(/^```/)) {
            // Insert blank line before current line
            improved.splice(-1, 0, '');
        }
        
        // After admonition end
        if (currentLine.trim() === ':::' && nextLine.trim() !== '') {
            improved.push('');
        }
        
        // Before admonition start (but not if previous line is already blank)
        if (currentLine.match(/^:::(note|tip|info|caution|danger)/) && prevLine.trim() !== '') {
            // Insert blank line before current line
            improved.splice(-1, 0, '');
        }
        
        // After tables (detect end of table)
        if (currentLine.match(/^\|.*\|$/) && !nextLine.match(/^\|.*\|$/) && nextLine.trim() !== '') {
            improved.push('');
        }
        
        // Before tables (detect start of table)
        if (currentLine.match(/^\|.*\|$/) && !prevLine.match(/^\|.*\|$/) && prevLine.trim() !== '' && !prevLine.match(/^#{1,6}\s+/)) {
            // Insert blank line before current line
            improved.splice(-1, 0, '');
        }
        
        // After frontmatter
        if (currentLine.trim() === '---' && i > 0 && nextLine.trim() !== '') {
            // Check if this is the closing frontmatter delimiter
            let frontmatterStart = -1;
            for (let j = i - 1; j >= 0; j--) {
                if (lines[j].trim() === '---') {
                    frontmatterStart = j;
                    break;
                }
            }
            if (frontmatterStart === 0) {
                improved.push('');
            }
        }
        
        // After list items that are followed by non-list content
        if (currentLine.match(/^[\s]*[-*+]\s+/) && !nextLine.match(/^[\s]*[-*+]\s+/) && nextLine.trim() !== '') {
            improved.push('');
        }
    }
    
    // Clean up multiple consecutive blank lines (limit to 2 max)
    let final = [];
    let blankCount = 0;
    
    for (const line of improved) {
        if (line.trim() === '') {
            blankCount++;
            if (blankCount <= 2) {
                final.push(line);
            }
        } else {
            blankCount = 0;
            final.push(line);
        }
    }
    
    // Remove trailing blank lines
    while (final.length > 0 && final[final.length - 1].trim() === '') {
        final.pop();
    }
    
    return final.join('\n') + '\n';
}

function processFile(filePath) {
    try {
        console.log(`Processing: ${filePath}`);
        const content = fs.readFileSync(filePath, 'utf8');
        const improved = improveMarkdownFormatting(content);
        fs.writeFileSync(filePath, improved, 'utf8');
    } catch (error) {
        console.error(`Error processing ${filePath}:`, error.message);
    }
}

function processDirectory(directory) {
    const files = fs.readdirSync(directory);
    
    for (const file of files) {
        const filePath = path.join(directory, file);
        const stat = fs.statSync(filePath);
        
        if (stat.isDirectory()) {
            processDirectory(filePath);
        } else if (path.extname(filePath) === '.md') {
            processFile(filePath);
        }
    }
}

function main() {
    console.log('Improving Markdown formatting for better editor readability...\n');
    
    const docsDir = '/Users/james.bognar/git/juneau/juneau-docs-poc/docs';
    
    if (fs.existsSync(docsDir)) {
        processDirectory(docsDir);
        console.log('\n✅ Markdown formatting improvements complete!');
    } else {
        console.error('❌ docs directory not found');
    }
}

if (require.main === module) {
    main();
}
