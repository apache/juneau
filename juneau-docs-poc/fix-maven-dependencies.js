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

function fixMavenDependency(content) {
    // Pattern to match malformed Maven dependency sections
    // Matches: ### Maven Dependency\n\n|<spaces>org.apache.juneau\n<artifactId>\n<version>
    const mavenPattern = /### Maven Dependency\s*\n\s*\n\s*\|\s*org\.apache\.juneau\s*\n([^\n]+)\s*\n([^\n]+)\s*\n/g;
    
    return content.replace(mavenPattern, (match, artifactId, version) => {
        // Clean up the artifact ID and version (remove any extra whitespace)
        const cleanArtifactId = artifactId.trim();
        const cleanVersion = version.trim();
        
        return `##### Maven Dependency

\`\`\`xml
<dependency>
    <groupId>org.apache.juneau</groupId>
    <artifactId>${cleanArtifactId}</artifactId>
    <version>${cleanVersion}</version>
</dependency>
\`\`\`

`;
    });
}

function fixJavaLibrary(content) {
    // Pattern to match: ### Java Library\n\n<jarname>
    const javaLibPattern = /### Java Library\s*\n\s*\n([^\n]+\.jar)\s*\n/g;
    
    return content.replace(javaLibPattern, (match, jarName) => {
        const cleanJarName = jarName.trim();
        
        return `##### Java Library

\`\`\`text
${cleanJarName}
\`\`\`

`;
    });
}

function fixOSGiModule(content) {
    // Pattern to match: ### OSGi Module\n\n<module-name>
    const osgiPattern = /### OSGi Module\s*\n\s*\n([^\n]+\.jar)\s*\n/g;
    
    return content.replace(osgiPattern, (match, moduleName) => {
        const cleanModuleName = moduleName.trim();
        
        return `##### OSGi Module

\`\`\`text
${cleanModuleName}
\`\`\`

`;
    });
}

function processFile(filePath) {
    try {
        console.log(`Processing: ${path.basename(filePath)}`);
        let content = fs.readFileSync(filePath, 'utf8');
        
        // Check if file contains Maven Dependency pattern that needs fixing
        if (content.includes('### Maven Dependency') && content.match(/\|\s*org\.apache\.juneau/)) {
            let modified = false;
            
            const originalContent = content;
            
            // Fix Maven dependency
            content = fixMavenDependency(content);
            if (content !== originalContent) {
                modified = true;
            }
            
            // Fix Java Library
            content = fixJavaLibrary(content);
            
            // Fix OSGi Module
            content = fixOSGiModule(content);
            
            if (modified) {
                fs.writeFileSync(filePath, content, 'utf8');
                console.log(`  ✅ Fixed Maven dependency structure in ${path.basename(filePath)}`);
            } else {
                console.log(`  ⏭️  No changes needed in ${path.basename(filePath)}`);
            }
        } else {
            console.log(`  ⏭️  No Maven dependency issues found in ${path.basename(filePath)}`);
        }
    } catch (error) {
        console.error(`❌ Error processing ${filePath}:`, error.message);
    }
}

// List of files that contain "Maven Dependency"
const filesToProcess = [
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/02.01.Juneau-marshall.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/03.01.Juneau-marshall-rdf.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/04.01.Juneau-dto.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/05.01.Juneau-config.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/06.01.Juneau-assertions.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/07.01.Juneau-rest-common.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/08.01.Juneau-rest-server.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/09.01.Juneau-rest-server-springboot.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/10.01.Juneau-rest-client.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/11.01.Juneau-rest-mock.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/12.01.Juneau-microservice-core.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/13.01.Juneau-microservice-jetty.md',
    '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics/16.03.Juneau-petstore-api.md'
];

console.log('Fixing Maven dependency structures in all affected files...\n');

let fixedCount = 0;
for (const filePath of filesToProcess) {
    if (fs.existsSync(filePath)) {
        const originalContent = fs.readFileSync(filePath, 'utf8');
        processFile(filePath);
        const newContent = fs.readFileSync(filePath, 'utf8');
        if (originalContent !== newContent) {
            fixedCount++;
        }
    } else {
        console.log(`❌ File not found: ${path.basename(filePath)}`);
    }
}

console.log(`\n✅ Fixed ${fixedCount} files with Maven dependency issues!`);
