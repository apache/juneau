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
 * This runs before the Docusaurus build to ensure all JUNEAU_VERSION and 
 * API_DOCS placeholders are replaced with actual values.
 */

const fs = require('fs');
const path = require('path');

// Configuration
const VERSION_VALUE = '9.0.1';
const API_DOCS_VALUE = '../site/apidocs';

console.log('🔧 Starting Juneau docs preprocessing...');
console.log(`📋 JUNEAU_VERSION: ${VERSION_VALUE}`);
console.log(`📋 API_DOCS: ${API_DOCS_VALUE}`);

/**
 * Replace placeholders in a string
 */
function replacePlaceholders(content) {
  return content
    .replace(/JUNEAU_VERSION/g, VERSION_VALUE)
    .replace(/API_DOCS/g, API_DOCS_VALUE);
}


/**
 * Recursively find all .md files in a directory
 */
function findMarkdownFiles(dir, fileList = []) {
  const files = fs.readdirSync(dir);
  
  for (const file of files) {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    
    if (stat.isDirectory()) {
      findMarkdownFiles(filePath, fileList);
    } else if (file.endsWith('.md')) {
      fileList.push(filePath);
    }
  }
  
  return fileList;
}

/**
 * Copy a file and process placeholders in the copy
 */
function copyAndProcessFile(sourcePath, targetPath) {
  try {
    // Ensure target directory exists
    const targetDir = path.dirname(targetPath);
    if (!fs.existsSync(targetDir)) {
      fs.mkdirSync(targetDir, { recursive: true });
    }
    
    const content = fs.readFileSync(sourcePath, 'utf8');
    
    // Check if file contains placeholders
    const hasPlaceholders = content.includes('JUNEAU_VERSION') || 
                           content.includes('API_DOCS');
    
    if (hasPlaceholders) {
      const processedContent = replacePlaceholders(content);
      fs.writeFileSync(targetPath, processedContent, 'utf8');
      console.log(`✅ Processed: ${path.relative(path.join(__dirname, '..'), sourcePath)}`);
      return true;
    } else {
      // Copy file as-is if no placeholders
      fs.copyFileSync(sourcePath, targetPath);
      return false;
    }
  } catch (error) {
    console.error(`❌ Error processing ${sourcePath}:`, error.message);
    return false;
  }
}

/**
 * Main preprocessing function
 */
function preprocessDocs() {
  const sourceDir = path.join(__dirname, '../docs');
  const stagingDir = path.join(__dirname, '../docs-staging');
  
  // Clean staging directory
  if (fs.existsSync(stagingDir)) {
    fs.rmSync(stagingDir, { recursive: true, force: true });
  }
  fs.mkdirSync(stagingDir, { recursive: true });
  
  // Find all markdown files
  const markdownFiles = findMarkdownFiles(sourceDir);
  
  console.log(`📁 Found ${markdownFiles.length} markdown files`);
  console.log(`📋 Copying from: ${sourceDir}`);
  console.log(`📋 Staging to: ${stagingDir}`);
  
  let processedCount = 0;
  
  for (const sourcePath of markdownFiles) {
    // Calculate target path in staging directory
    const relativePath = path.relative(sourceDir, sourcePath);
    const targetPath = path.join(stagingDir, relativePath);
    
    if (copyAndProcessFile(sourcePath, targetPath)) {
      processedCount++;
    }
  }
  
  console.log(`🎯 Preprocessed ${processedCount} files with placeholders`);
  console.log(`📁 Staging directory: ${stagingDir}`);
  console.log('✅ Juneau docs preprocessing complete!');
}

// Run if called directly
if (require.main === module) {
  preprocessDocs();
}

module.exports = { preprocessDocs, replacePlaceholders };
