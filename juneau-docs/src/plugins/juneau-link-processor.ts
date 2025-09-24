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

import type {LoadContext, Plugin} from '@docusaurus/types';

export interface JuneauLinkProcessorOptions {
  packageAbbreviations?: Record<string, string>;
  javadocBaseUrl?: string;
}

export default function juneauLinkProcessor(
  context: LoadContext,
  options: JuneauLinkProcessorOptions = {}
): Plugin<void> {
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

  return {
    name: 'juneau-link-processor',
    configureWebpack(config, isServer, utils) {
      return {
        module: {
          rules: [
            {
              test: /\.mdx?$/,
              enforce: 'pre' as const,
              use: [
                {
                  loader: require.resolve('string-replace-loader'),
                  options: {
                    multiple: [
                      // Process {@link package.Class#method method} patterns
                      {
                        search: /\{@link\s+([a-zA-Z0-9_.]+)#([a-zA-Z0-9_()]+)(?:\s+([^}]+))?\}/g,
                        replace: (match: string, className: string, method: string, displayText?: string) => {
                          const expandedClass = expandPackageAbbreviations(className, packageAbbreviations);
                          const classPath = expandedClass.replace(/\./g, '/');
                          const display = displayText || `${className.split('.').pop()}#${method}`;
                          return `[\`${display}\`](${javadocBaseUrl}/${classPath}.html#${method})`;
                        }
                      },
                      // Process {@link package.Class Class} patterns
                      {
                        search: /\{@link\s+([a-zA-Z0-9_.]+)(?:\s+([^}]+))?\}/g,
                        replace: (match: string, className: string, displayText?: string) => {
                          const expandedClass = expandPackageAbbreviations(className, packageAbbreviations);
                          const classPath = expandedClass.replace(/\./g, '/');
                          const display = displayText || className.split('.').pop();
                          return `[\`${display}\`](${javadocBaseUrl}/${classPath}.html)`;
                        }
                      },
                      // Process package abbreviations in regular text
                      ...Object.entries(packageAbbreviations).map(([abbrev, fullPackage]) => ({
                        search: new RegExp(`\\b${abbrev}\\.`, 'g'),
                        replace: `${fullPackage}.`
                      }))
                    ]
                  }
                }
              ]
            }
          ]
        }
      };
    }
  };
}

function expandPackageAbbreviations(className: string, abbreviations: Record<string, string>): string {
  for (const [abbrev, fullPackage] of Object.entries(abbreviations)) {
    if (className.startsWith(abbrev + '.')) {
      return className.replace(abbrev + '.', fullPackage + '.');
    }
  }
  return className;
}
