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

// Define the renaming rules
const renameRules = [
    // Module files
    { old: '02.01.Juneau-marshall.md', new: '02.01.Module-juneau-marshall.md', title: 'Module: juneau-marshall' },
    { old: '03.01.Juneau-marshall-rdf.md', new: '03.01.Module-juneau-marshall-rdf.md', title: 'Module: juneau-marshall-rdf' },
    { old: '04.01.Juneau-dto.md', new: '04.01.Module-juneau-dto.md', title: 'Module: juneau-dto' },
    { old: '05.01.Juneau-config.md', new: '05.01.Module-juneau-config.md', title: 'Module: juneau-config' },
    { old: '06.01.Juneau-assertions.md', new: '06.01.Module-juneau-assertions.md', title: 'Module: juneau-assertions' },
    { old: '07.01.Juneau-rest-common.md', new: '07.01.Module-juneau-rest-common.md', title: 'Module: juneau-rest-common' },
    { old: '08.01.Juneau-rest-server.md', new: '08.01.Module-juneau-rest-server.md', title: 'Module: juneau-rest-server' },
    { old: '09.01.Juneau-rest-server-springboot.md', new: '09.01.Module-juneau-rest-server-springboot.md', title: 'Module: juneau-rest-server-springboot' },
    { old: '10.01.Juneau-rest-client.md', new: '10.01.Module-juneau-rest-client.md', title: 'Module: juneau-rest-client' },
    { old: '11.01.Juneau-rest-mock.md', new: '11.01.Module-juneau-rest-mock.md', title: 'Module: juneau-rest-mock' },
    { old: '12.01.Juneau-microservice-core.md', new: '12.01.Module-juneau-microservice-core.md', title: 'Module: juneau-microservice-core' },
    { old: '13.01.Juneau-microservice-jetty.md', new: '13.01.Module-juneau-microservice-jetty.md', title: 'Module: juneau-microservice-jetty' },
    
    // Petstore files (these should be "App:" instead of "Module:")
    { old: '16.01.Juneau-petstore.md', new: '16.01.App-juneau-petstore.md', title: 'App: juneau-petstore' },
    { old: '16.03.Juneau-petstore-api.md', new: '16.03.App-juneau-petstore-api.md', title: 'App: juneau-petstore-api' },
    { old: '16.04.Juneau-petstore-client.md', new: '16.04.App-juneau-petstore-client.md', title: 'App: juneau-petstore-client' },
    { old: '16.05.Juneau-petstore-server.md', new: '16.05.App-juneau-petstore-server.md', title: 'App: juneau-petstore-server' }
];

const docsDir = '/Users/james.bognar/git/juneau/juneau-docs-poc/docs/topics';

function updateFileTitle(filePath, newTitle) {
    try {
        const content = fs.readFileSync(filePath, 'utf8');
        
        // Update the title in the frontmatter
        const updatedContent = content.replace(/^title:\s*["']?([^"'\n]+)["']?$/m, `title: "${newTitle}"`);
        
        fs.writeFileSync(filePath, updatedContent, 'utf8');
        return true;
    } catch (error) {
        console.error(`Error updating title in ${filePath}:`, error.message);
        return false;
    }
}

function renameFile(oldPath, newPath, newTitle) {
    try {
        // First update the title
        if (updateFileTitle(oldPath, newTitle)) {
            console.log(`  ✅ Updated title to "${newTitle}"`);
        }
        
        // Then rename the file
        fs.renameSync(oldPath, newPath);
        console.log(`  ✅ Renamed: ${path.basename(oldPath)} → ${path.basename(newPath)}`);
        return true;
    } catch (error) {
        console.error(`❌ Error renaming ${oldPath}:`, error.message);
        return false;
    }
}

console.log('Renaming module and app files...\n');

let renamedCount = 0;
const sidebarUpdates = [];

for (const rule of renameRules) {
    const oldPath = path.join(docsDir, rule.old);
    const newPath = path.join(docsDir, rule.new);
    
    console.log(`Processing: ${rule.old}`);
    
    if (fs.existsSync(oldPath)) {
        if (renameFile(oldPath, newPath, rule.title)) {
            renamedCount++;
            
            // Track sidebar ID changes
            const oldId = rule.old.replace('.md', '');
            const newId = rule.new.replace('.md', '');
            sidebarUpdates.push({ oldId, newId, label: rule.title });
        }
    } else {
        console.log(`  ⚠️  File not found: ${rule.old}`);
    }
    console.log('');
}

console.log(`✅ Successfully renamed ${renamedCount} files!`);

// Output sidebar update information
if (sidebarUpdates.length > 0) {
    console.log('\n📝 Sidebar updates needed:');
    console.log('The following ID and label changes need to be made in sidebars.ts:');
    
    for (const update of sidebarUpdates) {
        console.log(`  - id: 'topics/${update.oldId}' → 'topics/${update.newId}'`);
        console.log(`  - label: '...' → '${update.label}'`);
    }
}
