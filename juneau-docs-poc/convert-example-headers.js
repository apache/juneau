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

function convertExampleHeaders(content) {
    let lines = content.split('\n');
    let converted = [];
    
    for (let i = 0; i < lines.length; i++) {
        const currentLine = lines[i];
        const nextLine = i < lines.length - 1 ? lines[i + 1] : '';
        
        // Check for ### Example: or ### Examples: headers
        if (currentLine.match(/^### Examples?:?\s*$/)) {
            const isPlural = currentLine.includes('Examples');
            const exampleType = isPlural ? 'Examples' : 'Example';
            
            // Convert to admonition
            converted.push(`:::tip ${exampleType}`);
            converted.push('');
            
            // Skip the next line if it's blank (common pattern)
            if (nextLine.trim() === '') {
                i++; // Skip the blank line
            }
            
            // Look ahead to find the end of this example section
            let j = i + 1;
            let foundCodeBlocks = 0;
            let inCodeBlock = false;
            let exampleContent = [];
            
            while (j < lines.length) {
                const line = lines[j];
                
                // Track code blocks
                if (line.trim().startsWith('```')) {
                    if (!inCodeBlock) {
                        foundCodeBlocks++;
                        inCodeBlock = true;
                    } else {
                        inCodeBlock = false;
                    }
                }
                
                // Stop when we hit another header or significant structural element
                if (line.match(/^#{1,6}\s+/) && !inCodeBlock) {
                    break;
                }
                
                // Stop when we hit another admonition
                if (line.match(/^:::(note|tip|info|caution|danger)/) && !inCodeBlock) {
                    break;
                }
                
                // For single examples, stop after the first complete code block
                if (!isPlural && foundCodeBlocks >= 2 && !inCodeBlock) {
                    // Check if the next non-empty line is likely continuing the example
                    let nextNonEmpty = j + 1;
                    while (nextNonEmpty < lines.length && lines[nextNonEmpty].trim() === '') {
                        nextNonEmpty++;
                    }
                    
                    if (nextNonEmpty < lines.length) {
                        const nextContent = lines[nextNonEmpty];
                        // If it's a header or admonition, stop here
                        if (nextContent.match(/^#{1,6}\s+/) || nextContent.match(/^:::/)) {
                            break;
                        }
                    }
                }
                
                exampleContent.push(line);
                j++;
            }
            
            // Add the example content
            converted.push(...exampleContent);
            converted.push(':::');
            converted.push('');
            
            // Update the main loop counter
            i = j - 1;
        } else {
            converted.push(currentLine);
        }
    }
    
    return converted.join('\n');
}

function processFile(filePath) {
    try {
        console.log(`Processing: ${filePath}`);
        const content = fs.readFileSync(filePath, 'utf8');
        
        // Check if file contains example headers to convert
        if (content.match(/^### Examples?:?\s*$/m)) {
            const converted = convertExampleHeaders(content);
            fs.writeFileSync(filePath, converted, 'utf8');
            console.log(`  ✅ Converted example headers in ${path.basename(filePath)}`);
        } else {
            console.log(`  ⏭️  No example headers found in ${path.basename(filePath)}`);
        }
    } catch (error) {
        console.error(`❌ Error processing ${filePath}:`, error.message);
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
    console.log('Converting ### Example and ### Examples headers to admonition blocks...\n');
    
    const docsDir = '/Users/james.bognar/git/juneau/juneau-docs-poc/docs';
    
    if (fs.existsSync(docsDir)) {
        processDirectory(docsDir);
        console.log('\n✅ Example header conversion complete!');
    } else {
        console.error('❌ docs directory not found');
    }
}

if (require.main === module) {
    main();
}
