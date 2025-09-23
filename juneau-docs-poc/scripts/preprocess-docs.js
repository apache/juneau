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

/**
 * Preprocessor script to replace Juneau placeholders in markdown files
 * before Docusaurus processes them.
 * 
 * This runs before the Docusaurus build to ensure all {{JUNEAU_VERSION}} and 
 * {{API_DOCS}} placeholders are replaced with actual values.
 */

const fs = require('fs');
const path = require('path');
const glob = require('glob');

// Configuration
const JUNEAU_VERSION = '9.0.1';
const API_DOCS = '../apidocs';

console.log('🔧 Starting Juneau docs preprocessing...');
console.log(`📋 JUNEAU_VERSION: ${JUNEAU_VERSION}`);
console.log(`📋 API_DOCS: ${API_DOCS}`);

/**
 * Replace placeholders in a string
 */
function replacePlaceholders(content) {
  return content
    .replace(/\{\{JUNEAU_VERSION\}\}/g, JUNEAU_VERSION)
    .replace(/\{\{API_DOCS\}\}/g, API_DOCS)
    // Also handle potential single-brace variants
    .replace(/\{JUNEAU_VERSION\}/g, JUNEAU_VERSION)
    .replace(/\{API_DOCS\}/g, API_DOCS);
}

/**
 * Process a single markdown file
 */
function processFile(filePath) {
  try {
    const content = fs.readFileSync(filePath, 'utf8');
    
    // Check if file contains placeholders
    const hasPlaceholders = content.includes('{{JUNEAU_VERSION}}') || 
                           content.includes('{{API_DOCS}}') ||
                           content.includes('{JUNEAU_VERSION}') || 
                           content.includes('{API_DOCS}');
    
    if (hasPlaceholders) {
      const processedContent = replacePlaceholders(content);
      
      // Verify replacement worked
      const stillHasPlaceholders = processedContent.includes('{{JUNEAU_VERSION}}') || 
                                  processedContent.includes('{{API_DOCS}}');
      
      if (stillHasPlaceholders) {
        console.log(`⚠️  WARNING: ${filePath} still has unreplaced placeholders`);
      } else {
        console.log(`✅ Processed: ${filePath}`);
      }
      
      fs.writeFileSync(filePath, processedContent, 'utf8');
      return true;
    }
    
    return false;
  } catch (error) {
    console.error(`❌ Error processing ${filePath}:`, error.message);
    return false;
  }
}

/**
 * Main preprocessing function
 */
function preprocessDocs() {
  const docsDir = path.join(__dirname, '../docs');
  
  // Find all markdown files
  const markdownFiles = glob.sync('**/*.md', { cwd: docsDir });
  
  console.log(`📁 Found ${markdownFiles.length} markdown files`);
  
  let processedCount = 0;
  
  for (const file of markdownFiles) {
    const fullPath = path.join(docsDir, file);
    if (processFile(fullPath)) {
      processedCount++;
    }
  }
  
  console.log(`🎯 Preprocessed ${processedCount} files with placeholders`);
  console.log('✅ Juneau docs preprocessing complete!');
}

// Run if called directly
if (require.main === module) {
  preprocessDocs();
}

module.exports = { preprocessDocs, replacePlaceholders };
