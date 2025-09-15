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
 * Simple demonstration of {@link} tag processing for Juneau documentation
 * This script shows how to convert {@link} tags to Markdown links
 */

const fs = require('fs');
const path = require('path');

// Package abbreviations mapping
const packageAbbreviations = {
  'oaj': 'org.apache.juneau',
  'oajr': 'org.apache.juneau.rest',
  'oajrc': 'org.apache.juneau.rest.client',
  'oajrs': 'org.apache.juneau.rest.server',
  'oajrss': 'org.apache.juneau.rest.server.springboot',
  'oajrm': 'org.apache.juneau.rest.mock',
  'oajmc': 'org.apache.juneau.microservice.core',
  'oajmj': 'org.apache.juneau.microservice.jetty',
};

const javadocBaseUrl = '../apidocs';

function expandPackageAbbreviations(className, abbreviations) {
  for (const [abbrev, fullPackage] of Object.entries(abbreviations)) {
    if (className.startsWith(abbrev + '.')) {
      return className.replace(abbrev + '.', fullPackage + '.');
    }
  }
  return className;
}

function processJuneauLinks(content) {
  // Process {@link package.Class#method method} patterns
  content = content.replace(
    /\{@link\s+([a-zA-Z0-9_.]+)#([a-zA-Z0-9_()]+)(?:\s+([^}]+))?\}/g,
    (match, className, method, displayText) => {
      const expandedClass = expandPackageAbbreviations(className, packageAbbreviations);
      const classPath = expandedClass.replace(/\./g, '/');
      const display = displayText || `${className.split('.').pop()}#${method}`;
      return `[\`${display}\`](${javadocBaseUrl}/${classPath}.html#${method})`;
    }
  );

  // Process {@link package.Class Class} patterns
  content = content.replace(
    /\{@link\s+([a-zA-Z0-9_.]+)(?:\s+([^}]+))?\}/g,
    (match, className, displayText) => {
      const expandedClass = expandPackageAbbreviations(className, packageAbbreviations);
      const classPath = expandedClass.replace(/\./g, '/');
      const display = displayText || className.split('.').pop() || className;
      return `[\`${display}\`](${javadocBaseUrl}/${classPath}.html)`;
    }
  );

  return content;
}

// Test with sample content
const sampleContent = `
# Apache Juneau Overview

The {@link oaj.serializer.Serializer} class is the parent class of all serializers.
The {@link oaj.json.JsonSerializer JsonSerializer} class can be used to serialize POJOs into JSON notation.
The {@link oajr.servlet.BasicRestServlet} class is the entry point for your REST resources.
REST methods are annotated with {@link oajr.annotation.RestGet @RestGet}.
The {@link oajrc.RestClient#builder() builder()} method creates a new client builder.
`;

console.log('=== Original Content ===');
console.log(sampleContent);

console.log('\n=== Processed Content ===');
const processedContent = processJuneauLinks(sampleContent);
console.log(processedContent);

// Process files if arguments provided
if (process.argv.length > 2) {
  const filePath = process.argv[2];
  if (fs.existsSync(filePath)) {
    console.log(`\n=== Processing file: ${filePath} ===`);
    const content = fs.readFileSync(filePath, 'utf8');
    const processed = processJuneauLinks(content);
    
    const outputPath = filePath.replace(/\.md$/, '.processed.md');
    fs.writeFileSync(outputPath, processed);
    console.log(`Processed content written to: ${outputPath}`);
  } else {
    console.error(`File not found: ${filePath}`);
  }
}
