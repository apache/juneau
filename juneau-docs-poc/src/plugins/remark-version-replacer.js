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
 * Remark plugin to replace version and API docs placeholders with actual values.
 * This works inside code blocks and anywhere else in the markdown.
 */
function remarkVersionReplacer(options = {}) {
  const version = options.version || '9.0.1';
  const apiDocsUrl = options.apiDocsUrl || '../apidocs';
  
  return (tree) => {
    visit(tree, ['text', 'code'], (node) => {
      if (node.value) {
        // Replace {{JUNEAU_VERSION}} with the actual version
        node.value = node.value.replace(/\{\{JUNEAU_VERSION\}\}/g, version);
        // Replace {{API_DOCS}} with the actual API docs URL
        node.value = node.value.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
      }
    });
    
    visit(tree, 'code', (node) => {
      if (node.value) {
        // Also handle code blocks specifically
        node.value = node.value.replace(/\{\{JUNEAU_VERSION\}\}/g, version);
        node.value = node.value.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
      }
    });

    // Handle link nodes specifically for API docs replacements
    visit(tree, 'link', (node) => {
      if (node.url) {
        node.url = node.url.replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
      }
    });
  };
}

module.exports = remarkVersionReplacer;
