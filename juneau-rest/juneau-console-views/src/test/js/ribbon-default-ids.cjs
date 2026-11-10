/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * ribbon-default-ids.cjs - prints the stock ribbon default-id map of juneau-ribbon.js, so a Java test can compare
 * it with RibbonItem's.
 *
 * Usage:  node ribbon-default-ids.cjs <juneau-ribbon.js>
 */
'use strict';

const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const ribbonJsPath = process.argv[2];
if (!ribbonJsPath) {
	console.error('usage: node ribbon-default-ids.cjs <juneau-ribbon.js>');
	process.exit(2);
}
const window = { document: { createElement: () => ({}) }, console: console };
window.window = window;
const sandbox = vm.createContext(window);
vm.runInContext(fs.readFileSync(path.resolve(ribbonJsPath), 'utf8'), sandbox, { filename: 'juneau-ribbon.js' }); // NOSONAR javascript:S1523 - loads the shipped script into a vm sandbox for the test
process.stdout.write(JSON.stringify({ ids: window.JuneauViews.ribbon.stockDefaultIds }));
