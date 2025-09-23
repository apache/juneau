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

// No AST traversal needed - we use string replacement instead

/**
 * Simple string replacement function that handles all placeholder replacements
 */
function replaceInString(content, version, apiDocsUrl) {
  return content
    .replace(/\{\{JUNEAU_VERSION\}\}/g, version)
    .replace(/\{\{API_DOCS\}\}/g, apiDocsUrl)
    // Also handle any potential single-brace variants that might cause MDX issues
    .replace(/\{JUNEAU_VERSION\}/g, version)
    .replace(/\{API_DOCS\}/g, apiDocsUrl);
}

/**
 * Remark plugin to replace version and API docs placeholders with actual values.
 * Simple string replacement approach that works on the entire file content.
 */
function remarkVersionReplacer(options = {}) {
  const version = options.version || '9.0.1';
  const apiDocsUrl = options.apiDocsUrl || '../apidocs';
  
  console.log(`🔧 remarkVersionReplacer initialized with version: ${version}, apiDocsUrl: ${apiDocsUrl}`);
  
  return (tree, file) => {
    const fileName = file.path || 'unknown';
    console.log(`📄 Processing file: ${fileName}`);
    
    // Try different possible content properties
    const content = file.contents || file.value || file.data;
    
    // Show content sample for files that should have placeholders
    if (fileName.includes('10.01.00.JuneauRestClientBasics')) {
      console.log(`🔍 SPECIFIC FILE DEBUG - ${fileName}:`);
      console.log(`📋 File object keys: ${Object.keys(file).join(', ')}`);
      console.log(`📋 Content type: ${typeof content}`);
      if (content) {
        const contentStr = String(content);
        console.log(`📋 Content length: ${contentStr.length}`);
        console.log(`📋 First 200 chars: ${contentStr.substring(0, 200)}`);
        console.log(`📋 Contains {{API_DOCS}}: ${contentStr.includes('{{API_DOCS}}')}`);
        console.log(`📋 Contains API_DOCS (no braces): ${contentStr.includes('API_DOCS')}`);
        console.log(`📋 Contains {API_DOCS}: ${contentStr.includes('{API_DOCS}')}`);
      }
    }
    
    if (content) {
      const hasApiDocs = content.includes('{{API_DOCS}}');
      const hasVersion = content.includes('{{JUNEAU_VERSION}}');
      
      if (hasApiDocs || hasVersion) {
        console.log(`🎯 Found placeholders in ${file.path || 'unknown'}: API_DOCS=${hasApiDocs}, VERSION=${hasVersion}`);
      }
      
      const replacedContent = replaceInString(content, version, apiDocsUrl);
      
      // Update the content property we found
      if (file.contents) file.contents = replacedContent;
      if (file.value) file.value = replacedContent;
      if (file.data) file.data = replacedContent;
      
      const stillHasPlaceholders = replacedContent.includes('{{API_DOCS}}') || replacedContent.includes('{{JUNEAU_VERSION}}');
      
      // Check for problematic single-brace or bare references that MDX might interpret as JS
      const hasSingleBraceApiDocs = replacedContent.includes('{API_DOCS}');
      const hasBareApiDocs = replacedContent.match(/[^{]API_DOCS[^}]/);
      
      if (fileName.includes('10.01.00.JuneauRestClientBasics')) {
        console.log(`🔍 POST-REPLACEMENT DEBUG - ${fileName}:`);
        console.log(`📋 Still has {{API_DOCS}}: ${replacedContent.includes('{{API_DOCS}}')}`);
        console.log(`📋 Has single {API_DOCS}: ${hasSingleBraceApiDocs}`);
        console.log(`📋 Has bare API_DOCS: ${!!hasBareApiDocs}`);
        if (hasBareApiDocs) {
          console.log(`📋 Bare API_DOCS context: ${hasBareApiDocs[0]}`);
        }
      }
      
      if (stillHasPlaceholders) {
        console.log(`⚠️  WARNING: Still has unreplaced placeholders in ${file.path || 'unknown'}`);
      } else if (hasApiDocs || hasVersion) {
        console.log(`✅ Successfully replaced placeholders in ${file.path || 'unknown'}`);
      }
      
      if (hasSingleBraceApiDocs || hasBareApiDocs) {
        console.log(`🚨 POTENTIAL MDX ISSUE: Found problematic API_DOCS reference in ${file.path || 'unknown'}`);
      }
    }
    // No need for AST traversal since string replacement handles all cases
  };
}

module.exports = remarkVersionReplacer;
