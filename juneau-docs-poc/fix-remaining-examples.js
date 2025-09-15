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

function fixRemainingExamples(content) {
    // Handle "### Example configuration file:" and similar patterns
    return content.replace(
        /^### Example ([^:]*):?\s*$/gm,
        ':::tip Example $1'
    ).replace(
        /^### Examples ([^:]*):?\s*$/gm,
        ':::tip Examples $1'
    );
}

// List of files that still have unconverted example headers
const filesToFix = [
    'docs/topics/01.04.RestServer.md',
    'docs/topics/13.06.Config.md', 
    'docs/topics/08.02.04.Deployment.md',
    'docs/topics/05.10.03.CustomStores.md',
    'docs/topics/05.02.Overview.md',
    'docs/topics/04.03.Atom.md',
    'docs/topics/01.07.ConfigFiles.md',
    'docs/topics/02.26.04.XmlBeanTypeNameAnnotation.md'
];

const baseDir = '/Users/james.bognar/git/juneau/juneau-docs-poc';

console.log('Fixing remaining example headers...\n');

for (const file of filesToFix) {
    const filePath = path.join(baseDir, file);
    
    if (fs.existsSync(filePath)) {
        console.log(`Processing: ${file}`);
        const content = fs.readFileSync(filePath, 'utf8');
        
        if (content.match(/^### Examples?\b/m)) {
            const fixed = fixRemainingExamples(content);
            fs.writeFileSync(filePath, fixed, 'utf8');
            console.log(`  ✅ Fixed remaining example headers`);
        } else {
            console.log(`  ⏭️  No remaining example headers found`);
        }
    } else {
        console.log(`❌ File not found: ${file}`);
    }
}

console.log('\n✅ Remaining example header fixes complete!');
