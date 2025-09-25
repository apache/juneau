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
const { execSync } = require('child_process');
const http = require('http');
const https = require('https');

// Configuration
const CONFIG = {
  // Local server configuration
  localServerUrl: 'http://localhost:3000',
  localServerPort: 3000,
  
  // Directories to check
  docsDir: './docs-staging',
  staticDir: './static',
  srcDir: './src',
  
  // File patterns to check
  filePatterns: ['.md', '.mdx', '.html', '.tsx', '.ts', '.js'],
  
  // Link patterns to check
  linkPatterns: [
    /\[([^\]]+)\]\(([^)]+)\)/g,  // Markdown links [text](url)
    /href=["']([^"']+)["']/g,    // HTML href attributes
    /src=["']([^"']+)["']/g,     // HTML src attributes
    /to=["']([^"']+)["']/g,      // React Router to attributes
  ],
  
  // URLs to skip (external links that might be flaky)
  skipUrls: [
    'mailto:',
    'javascript:',
    'data:',
    '#', // Fragment-only links
  ],
  
  // File extensions that should exist as files
  fileExtensions: ['.html', '.md', '.png', '.jpg', '.jpeg', '.gif', '.svg', '.css', '.js', '.pdf', '.txt'],
  
  // Timeout for HTTP requests (ms)
  httpTimeout: 10000,
  
  // Maximum concurrent requests
  maxConcurrent: 10,
};

class BrokenLinkChecker {
  constructor() {
    this.brokenLinks = [];
    this.checkedUrls = new Set();
    this.pendingRequests = 0;
    this.maxConcurrent = CONFIG.maxConcurrent;
  }

  async checkAll() {
    console.log('🔍 Starting broken link check for Juneau documentation...\n');
    
    // Check if local server is running
    if (!await this.isLocalServerRunning()) {
      console.log('⚠️  Local server not running. Starting it now...');
      await this.startLocalServer();
    }
    
    // Find all files to check
    const files = this.findFilesToCheck();
    console.log(`📁 Found ${files.length} files to check\n`);
    
    // Check each file
    for (const file of files) {
      await this.checkFile(file);
    }
    
    // Wait for all pending requests to complete
    while (this.pendingRequests > 0) {
      await this.sleep(100);
    }
    
    // Generate report
    this.generateReport();
  }

  findFilesToCheck() {
    const files = [];
    
    // Check docs directory
    if (fs.existsSync(CONFIG.docsDir)) {
      files.push(...this.findFilesInDirectory(CONFIG.docsDir));
    }
    
    // Check src directory
    if (fs.existsSync(CONFIG.srcDir)) {
      files.push(...this.findFilesInDirectory(CONFIG.srcDir));
    }
    
    return files;
  }

  findFilesInDirectory(dir) {
    const files = [];
    
    const walkDir = (currentDir) => {
      const items = fs.readdirSync(currentDir);
      
      for (const item of items) {
        const fullPath = path.join(currentDir, item);
        const stat = fs.statSync(fullPath);
        
        if (stat.isDirectory()) {
          // Skip node_modules and other build directories
          if (!['node_modules', '.docusaurus', 'build', '.git'].includes(item)) {
            walkDir(fullPath);
          }
        } else if (stat.isFile()) {
          const ext = path.extname(item);
          if (CONFIG.filePatterns.includes(ext)) {
            files.push(fullPath);
          }
        }
      }
    };
    
    walkDir(dir);
    return files;
  }

  async checkFile(filePath) {
    try {
      const content = fs.readFileSync(filePath, 'utf8');
      const links = this.extractLinks(content, filePath);
      
      if (links.length > 0) {
        console.log(`🔗 Checking ${links.length} links in ${filePath}`);
        
        for (const link of links) {
          await this.checkLink(link, filePath);
        }
      }
    } catch (error) {
      console.error(`❌ Error reading file ${filePath}:`, error.message);
    }
  }

  extractLinks(content, filePath) {
    const links = [];
    
    for (const pattern of CONFIG.linkPatterns) {
      let match;
      while ((match = pattern.exec(content)) !== null) {
        const url = match[2] || match[1]; // For different patterns, URL might be in different groups
        const lineNumber = content.substring(0, match.index).split('\n').length;
        
        links.push({
          url: url,
          text: match[1] || '',
          line: lineNumber,
          file: filePath,
          fullMatch: match[0]
        });
      }
    }
    
    return links;
  }

  async checkLink(link, filePath) {
    const url = link.url;
    
    // Skip certain URLs
    if (CONFIG.skipUrls.some(skip => url.startsWith(skip))) {
      return;
    }
    
    // Skip if already checked
    if (this.checkedUrls.has(url)) {
      return;
    }
    
    this.checkedUrls.add(url);
    
    // Wait if we have too many concurrent requests
    while (this.pendingRequests >= this.maxConcurrent) {
      await this.sleep(50);
    }
    
    this.pendingRequests++;
    
    try {
      const isBroken = await this.isLinkBroken(url, filePath);
      if (isBroken) {
        this.brokenLinks.push({
          ...link,
          file: filePath,
          reason: isBroken
        });
      }
    } catch (error) {
      this.brokenLinks.push({
        ...link,
        file: filePath,
        reason: `Error: ${error.message}`
      });
    } finally {
      this.pendingRequests--;
    }
  }

  async isLinkBroken(url, filePath) {
    // Handle relative URLs
    if (url.startsWith('/') || url.startsWith('./') || url.startsWith('../')) {
      return this.checkRelativeLink(url, filePath);
    }
    
    // Handle absolute URLs
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return this.checkHttpLink(url);
    }
    
    // Handle local file references
    if (url.includes('.') && !url.startsWith('#')) {
      return this.checkLocalFile(url, filePath);
    }
    
    return false; // Assume fragment links are valid
  }

  checkRelativeLink(url, filePath) {
    try {
      // Convert relative URL to absolute path
      const baseDir = path.dirname(filePath);
      let targetPath;
      
      if (url.startsWith('/')) {
        // Absolute path from project root
        targetPath = path.join(process.cwd(), url);
      } else {
        // Relative path
        targetPath = path.resolve(baseDir, url);
      }
      
      // Check if file exists
      if (fs.existsSync(targetPath)) {
        return false; // Link is valid
      }
      
      // Check if it's a directory with index file
      const indexFiles = ['index.html', 'index.md', 'README.md'];
      for (const indexFile of indexFiles) {
        if (fs.existsSync(path.join(targetPath, indexFile))) {
          return false; // Link is valid
        }
      }
      
      return `File not found: ${targetPath}`;
    } catch (error) {
      return `Error checking relative link: ${error.message}`;
    }
  }

  async checkHttpLink(url) {
    return new Promise((resolve) => {
      const client = url.startsWith('https://') ? https : http;
      const timeout = setTimeout(() => {
        resolve(`Timeout after ${CONFIG.httpTimeout}ms`);
      }, CONFIG.httpTimeout);
      
      const req = client.get(url, (res) => {
        clearTimeout(timeout);
        if (res.statusCode >= 200 && res.statusCode < 400) {
          resolve(false); // Link is valid
        } else {
          resolve(`HTTP ${res.statusCode}: ${res.statusMessage}`);
        }
      });
      
      req.on('error', (error) => {
        clearTimeout(timeout);
        resolve(`Network error: ${error.message}`);
      });
      
      req.setTimeout(CONFIG.httpTimeout, () => {
        req.destroy();
        resolve(`Request timeout`);
      });
    });
  }

  checkLocalFile(url, filePath) {
    try {
      const baseDir = path.dirname(filePath);
      const targetPath = path.resolve(baseDir, url);
      
      if (fs.existsSync(targetPath)) {
        return false; // File exists
      }
      
      return `Local file not found: ${targetPath}`;
    } catch (error) {
      return `Error checking local file: ${error.message}`;
    }
  }

  async isLocalServerRunning() {
    return new Promise((resolve) => {
      const req = http.get(`${CONFIG.localServerUrl}/`, (res) => {
        resolve(true);
      });
      
      req.on('error', () => {
        resolve(false);
      });
      
      req.setTimeout(2000, () => {
        req.destroy();
        resolve(false);
      });
    });
  }

  async startLocalServer() {
    console.log('🚀 Starting local Docusaurus server...');
    
    try {
      // Start server in background
      const child = execSync('npm start', { 
        cwd: process.cwd(),
        stdio: 'pipe',
        detached: true 
      });
      
      // Wait for server to start
      let attempts = 0;
      while (attempts < 30) {
        if (await this.isLocalServerRunning()) {
          console.log('✅ Local server is running');
          return;
        }
        await this.sleep(1000);
        attempts++;
      }
      
      throw new Error('Server failed to start within 30 seconds');
    } catch (error) {
      console.error('❌ Failed to start local server:', error.message);
      process.exit(1);
    }
  }

  generateReport() {
    console.log('\n' + '='.repeat(80));
    console.log('📊 BROKEN LINK REPORT');
    console.log('='.repeat(80));
    
    if (this.brokenLinks.length === 0) {
      console.log('✅ No broken links found!');
      return;
    }
    
    console.log(`❌ Found ${this.brokenLinks.length} broken links:\n`);
    
    // Group by file
    const byFile = {};
    for (const link of this.brokenLinks) {
      if (!byFile[link.file]) {
        byFile[link.file] = [];
      }
      byFile[link.file].push(link);
    }
    
    // Report by file
    for (const [file, links] of Object.entries(byFile)) {
      console.log(`📄 ${file}:`);
      for (const link of links) {
        console.log(`   Line ${link.line}: ${link.url}`);
        console.log(`   Text: "${link.text}"`);
        console.log(`   Reason: ${link.reason}`);
        console.log('');
      }
    }
    
    // Summary
    console.log('='.repeat(80));
    console.log(`📈 SUMMARY:`);
    console.log(`   Total files checked: ${this.findFilesToCheck().length}`);
    console.log(`   Total links checked: ${this.checkedUrls.size}`);
    console.log(`   Broken links found: ${this.brokenLinks.length}`);
    console.log('='.repeat(80));
  }

  sleep(ms) {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

// Run the checker
if (require.main === module) {
  const checker = new BrokenLinkChecker();
  checker.checkAll().catch(error => {
    console.error('❌ Fatal error:', error);
    process.exit(1);
  });
}

module.exports = BrokenLinkChecker;
