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
    .replace(/\{\{API_DOCS\}\}/g, apiDocsUrl);
}

/**
 * Remark plugin to replace version and API docs placeholders with actual values.
 * Simple string replacement approach that works on the entire file content.
 */
function remarkVersionReplacer(options = {}) {
  const version = options.version || '9.0.1';
  const apiDocsUrl = options.apiDocsUrl || '../apidocs';
  
  return (tree, file) => {
    // Replace placeholders in the entire file content before parsing
    if (file.contents) {
      file.contents = replaceInString(file.contents, version, apiDocsUrl);
    }
    // No need for AST traversal since string replacement handles all cases
  };
}

module.exports = remarkVersionReplacer;
