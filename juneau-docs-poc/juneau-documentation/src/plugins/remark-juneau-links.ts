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

import { visit } from 'unist-util-visit';
import type { Plugin } from 'unified';
import type { Root } from 'mdast';

export interface JuneauLinkOptions {
  packageAbbreviations?: Record<string, string>;
  javadocBaseUrl?: string;
}

const remarkJuneauLinks: Plugin<[JuneauLinkOptions?], Root> = (options = {}) => {
  const {
    packageAbbreviations = {
      'oaj': 'org.apache.juneau',
      'oajr': 'org.apache.juneau.rest',
      'oajrc': 'org.apache.juneau.rest.client',
      'oajrs': 'org.apache.juneau.rest.server',
      'oajrss': 'org.apache.juneau.rest.server.springboot',
      'oajrm': 'org.apache.juneau.rest.mock',
      'oajmc': 'org.apache.juneau.microservice.core',
      'oajmj': 'org.apache.juneau.microservice.jetty',
    },
    javadocBaseUrl = '../apidocs'
  } = options;

  return (tree) => {
    visit(tree, 'text', (node, index, parent) => {
      if (!node.value || !parent || index === undefined) return;

      const text = node.value;
      
      // Process {@link} patterns
      const linkPattern = /\{@link\s+([a-zA-Z0-9_.]+)(?:#([a-zA-Z0-9_()]+))?(?:\s+([^}]+))?\}/g;
      
      if (linkPattern.test(text)) {
        const newNodes: any[] = [];
        let lastIndex = 0;
        
        text.replace(linkPattern, (match, className, method, displayText, offset) => {
          // Add text before the match
          if (offset > lastIndex) {
            newNodes.push({
              type: 'text',
              value: text.slice(lastIndex, offset)
            });
          }
          
          // Expand package abbreviations
          const expandedClass = expandPackageAbbreviations(className, packageAbbreviations);
          const classPath = expandedClass.replace(/\./g, '/');
          
          // Determine display text and URL
          let display: string;
          let url: string;
          
          if (method) {
            display = displayText || `${className.split('.').pop()}#${method}`;
            url = `${javadocBaseUrl}/${classPath}.html#${method}`;
          } else {
            display = displayText || className.split('.').pop() || className;
            url = `${javadocBaseUrl}/${classPath}.html`;
          }
          
          // Create link node
          newNodes.push({
            type: 'link',
            url: url,
            children: [{
              type: 'inlineCode',
              value: display
            }]
          });
          
          lastIndex = offset + match.length;
          return match;
        });
        
        // Add remaining text
        if (lastIndex < text.length) {
          newNodes.push({
            type: 'text',
            value: text.slice(lastIndex)
          });
        }
        
        // Replace the text node with the new nodes
        if (newNodes.length > 0) {
          parent.children.splice(index, 1, ...newNodes);
        }
      }
    });
  };
};

function expandPackageAbbreviations(className: string, abbreviations: Record<string, string>): string {
  for (const [abbrev, fullPackage] of Object.entries(abbreviations)) {
    if (className.startsWith(abbrev + '.')) {
      return className.replace(abbrev + '.', fullPackage + '.');
    }
  }
  return className;
}

export default remarkJuneauLinks;
