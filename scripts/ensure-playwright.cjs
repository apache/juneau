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
'use strict';
/*
 * Makes sure the browser tests' pinned npm packages are in <dir>/node_modules without running npm when it can be avoided:
 *
 *     node ensure-playwright.cjs <dir> <npm> <name@version>...
 *
 * 1. Every spec already installed at its pinned version: print "already installed" and stop.
 * 2. Else a per-user cache holds this exact spec set (~/.cache/juneau-js-tests/<hash of the sorted specs>/node_modules):
 *    copy it into <dir>.  A full build runs "mvn clean" first, which removes <dir>, so the cache is what makes the
 *    second and later full runs skip npm.
 * 3. Else run "<npm> install --no-fund --no-audit --prefix <dir> <specs...>" and fill the cache from the result.
 *
 * The cache is keyed by the whole spec set, so a petstore entry never satisfies the views module's larger set, or the
 * reverse.  Exits with npm's status when it had to run it.
 */
const fs = require('fs');
const os = require('os');
const path = require('path');
const crypto = require('crypto');
const { spawnSync } = require('child_process');

const [dir, npm, ...specs] = process.argv.slice(2);
if (!dir || !npm || specs.length === 0) {
	process.stderr.write('usage: node ensure-playwright.cjs <dir> <npm> <name@version>...\n');
	process.exit(2);
}

/** @return {{name: string, version: string}} A spec such as "playwright@1.49.1" or "@scope/pkg@1.0.0". */
function parse(spec) {
	const at = spec.lastIndexOf('@');
	return { name: spec.slice(0, at), version: spec.slice(at + 1) };
}

/** @return {boolean} Whether every spec is installed under {@code modules} at exactly its version. */
function installed(modules) {
	return specs.every(spec => {
		const { name, version } = parse(spec);
		try {
			return JSON.parse(fs.readFileSync(path.join(modules, name, 'package.json'), 'utf8')).version === version;
		} catch (e) {
			return false;
		}
	});
}

const modules = path.join(dir, 'node_modules');
if (installed(modules)) {
	process.stdout.write('already installed: ' + specs.join(' ') + '\n');
	process.exit(0);
}

const key = crypto.createHash('sha256').update([...specs].sort().join('\n')).digest('hex').slice(0, 16);
const cacheRoot = process.env.JUNEAU_JS_TESTS_CACHE || path.join(os.homedir(), '.cache', 'juneau-js-tests');
const cached = path.join(cacheRoot, key, 'node_modules');

if (installed(cached)) {
	fs.rmSync(modules, { recursive: true, force: true });
	fs.mkdirSync(dir, { recursive: true });
	fs.cpSync(cached, modules, { recursive: true, verbatimSymlinks: true });
	if (installed(modules)) {
		process.stdout.write('restored from cache ' + cached + ': ' + specs.join(' ') + '\n');
		process.exit(0);
	}
}

const r = spawnSync(npm, ['install', '--no-fund', '--no-audit', '--prefix', dir, ...specs],
	{ stdio: 'inherit', shell: process.platform === 'win32' });
if (r.status !== 0)
	process.exit(r.status === null ? 1 : r.status);

// Fill the cache through a sibling that only this process writes, then move it into place.  Losing the rename race to
// another process that filled the same entry first is fine: its copy is as good as ours.
try {
	const target = path.join(cacheRoot, key);
	const tmp = path.join(cacheRoot, key + '.tmp-' + process.pid);
	fs.mkdirSync(tmp, { recursive: true });
	fs.cpSync(modules, path.join(tmp, 'node_modules'), { recursive: true, verbatimSymlinks: true });
	try {
		fs.renameSync(tmp, target);
	} catch (e) {
		fs.rmSync(tmp, { recursive: true, force: true });
		if (e.code !== 'ENOTEMPTY' && e.code !== 'EEXIST')
			throw e;
	}
} catch (e) {
	// The cache is an optimisation; the install itself succeeded.
	process.stderr.write('could not fill the cache: ' + e.message + '\n');
}
process.exit(0);
