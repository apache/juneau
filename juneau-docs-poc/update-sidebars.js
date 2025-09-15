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

const sidebarFile = '/Users/james.bognar/git/juneau/juneau-docs-poc/sidebars.ts';

// Define the updates needed
const updates = [
    // Module files
    { oldId: 'topics/02.01.Juneau-marshall', newId: 'topics/02.01.Module-juneau-marshall', newLabel: 'Module: juneau-marshall' },
    { oldId: 'topics/03.01.Juneau-marshall-rdf', newId: 'topics/03.01.Module-juneau-marshall-rdf', newLabel: 'Module: juneau-marshall-rdf' },
    { oldId: 'topics/04.01.Juneau-dto', newId: 'topics/04.01.Module-juneau-dto', newLabel: 'Module: juneau-dto' },
    { oldId: 'topics/05.01.Juneau-config', newId: 'topics/05.01.Module-juneau-config', newLabel: 'Module: juneau-config' },
    { oldId: 'topics/06.01.Juneau-assertions', newId: 'topics/06.01.Module-juneau-assertions', newLabel: 'Module: juneau-assertions' },
    { oldId: 'topics/07.01.Juneau-rest-common', newId: 'topics/07.01.Module-juneau-rest-common', newLabel: 'Module: juneau-rest-common' },
    { oldId: 'topics/08.01.Juneau-rest-server', newId: 'topics/08.01.Module-juneau-rest-server', newLabel: 'Module: juneau-rest-server' },
    { oldId: 'topics/09.01.Juneau-rest-server-springboot', newId: 'topics/09.01.Module-juneau-rest-server-springboot', newLabel: 'Module: juneau-rest-server-springboot' },
    { oldId: 'topics/10.01.Juneau-rest-client', newId: 'topics/10.01.Module-juneau-rest-client', newLabel: 'Module: juneau-rest-client' },
    { oldId: 'topics/11.01.Juneau-rest-mock', newId: 'topics/11.01.Module-juneau-rest-mock', newLabel: 'Module: juneau-rest-mock' },
    { oldId: 'topics/12.01.Juneau-microservice-core', newId: 'topics/12.01.Module-juneau-microservice-core', newLabel: 'Module: juneau-microservice-core' },
    { oldId: 'topics/13.01.Juneau-microservice-jetty', newId: 'topics/13.01.Module-juneau-microservice-jetty', newLabel: 'Module: juneau-microservice-jetty' },
    
    // App files (petstore)
    { oldId: 'topics/16.01.Juneau-petstore', newId: 'topics/16.01.App-juneau-petstore', newLabel: 'App: juneau-petstore' },
    { oldId: 'topics/16.03.Juneau-petstore-api', newId: 'topics/16.03.App-juneau-petstore-api', newLabel: 'App: juneau-petstore-api' },
    { oldId: 'topics/16.04.Juneau-petstore-client', newId: 'topics/16.04.App-juneau-petstore-client', newLabel: 'App: juneau-petstore-client' },
    { oldId: 'topics/16.05.Juneau-petstore-server', newId: 'topics/16.05.App-juneau-petstore-server', newLabel: 'App: juneau-petstore-server' }
];

function updateSidebar() {
    try {
        let content = fs.readFileSync(sidebarFile, 'utf8');
        let updateCount = 0;
        
        for (const update of updates) {
            // Update the ID
            const idPattern = new RegExp(`id:\\s*['"]${update.oldId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"]`, 'g');
            if (content.match(idPattern)) {
                content = content.replace(idPattern, `id: '${update.newId}'`);
                console.log(`  ✅ Updated ID: ${update.oldId} → ${update.newId}`);
                updateCount++;
            }
            
            // Find and update the corresponding label
            // Look for the label that comes after this ID
            const sections = content.split(idPattern);
            if (sections.length > 1) {
                // Find the label in the same block
                const labelPattern = /label:\s*['"]([^'"]+)['"]/;
                const afterId = sections[1];
                const labelMatch = afterId.match(labelPattern);
                if (labelMatch) {
                    const currentLabel = labelMatch[1];
                    // Only update if the label looks like it needs updating
                    if (currentLabel.includes('Juneau') && !currentLabel.startsWith('Module:') && !currentLabel.startsWith('App:')) {
                        content = content.replace(
                            new RegExp(`(id:\\s*['"]${update.newId.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"][^}]*label:\\s*)['"]${currentLabel.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}['"]`),
                            `$1'${update.newLabel}'`
                        );
                        console.log(`  ✅ Updated label: "${currentLabel}" → "${update.newLabel}"`);
                    }
                }
            }
        }
        
        fs.writeFileSync(sidebarFile, content, 'utf8');
        console.log(`\n✅ Updated sidebars.ts with ${updateCount} ID changes!`);
        return true;
    } catch (error) {
        console.error('❌ Error updating sidebars.ts:', error.message);
        return false;
    }
}

console.log('Updating sidebars.ts with new file references...\n');
updateSidebar();
