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

const { visit } = require('unist-util-visit');

/**
 * Simple string replacement function as fallback
 */
function replaceInString(content, version, apiDocsUrl) {
  return content
    .replace(/\{\{JUNEAU_VERSION\}\}/g, version)
    .replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
}

/**
 * Remark plugin to replace version and API docs placeholders with actual values.
 * This works inside code blocks and anywhere else in the markdown.
 */
function remarkVersionReplacer(options = {}) {
  const version = options.version || '9.0.1';
  const apiDocsUrl = options.apiDocsUrl || '../apidocs';
  
  return (tree, file) => {
    // First, do a string-level replacement on the entire file content
    if (file.contents) {
      file.contents = replaceInString(file.contents, version, apiDocsUrl);
    }
    // Process all nodes that might contain text content
    visit(tree, (node) => {
      // Handle text nodes
      if (node.type === 'text' && node.value) {
        node.value = node.value.replace(/\{\{JUNEAU_VERSION\}\}/g, version);
        node.value = node.value.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
      }
      
      // Handle code nodes
      if (node.type === 'code' && node.value) {
        node.value = node.value.replace(/\{\{JUNEAU_VERSION\}\}/g, version);
        node.value = node.value.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
      }
      
      // Handle inline code nodes
      if (node.type === 'inlineCode' && node.value) {
        node.value = node.value.replace(/\{\{JUNEAU_VERSION\}\}/g, version);
        node.value = node.value.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
      }
      
      // Handle link nodes
      if (node.type === 'link' && node.url) {
        node.url = node.url.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
      }
      
      // Handle HTML/JSX nodes (like our custom components)
      if (node.type === 'html' && node.value) {
        node.value = node.value.replace(/\{\{JUNEAU_VERSION\}\}/g, version);
        node.value = node.value.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
      }
      
      // Handle MDX JSX elements
      if (node.type === 'mdxJsxTextElement' || node.type === 'mdxJsxFlowElement') {
        // Process children of JSX elements
        if (node.children) {
          node.children.forEach(child => {
            if (child.type === 'text' && child.value) {
              child.value = child.value.replace(/\{\{JUNEAU_VERSION\}\}/g, version);
              child.value = child.value.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
            }
          });
        }
        
        // Process attributes
        if (node.attributes) {
          node.attributes.forEach(attr => {
            if (attr.value && typeof attr.value === 'string') {
              attr.value = attr.value.replace(/\{\{JUNEAU_VERSION\}\}/g, version);
              attr.value = attr.value.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
            }
          });
        }
      }
    });
  };
}

module.exports = remarkVersionReplacer;
