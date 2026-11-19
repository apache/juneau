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
 * node-floor-preload.cjs - makes a newer local Node behave like the oldest Node the build supports (the "Node floor",
 * currently 18; see juneau.node.floor in the root pom) by deleting every builtin that was added after it.  A harness or
 * shipped script that calls one then fails locally with a TypeError, exactly as it would on the CI agent, instead of
 * passing on a developer machine and failing only in CI.
 *
 * Injected into every test JVM's child processes through NODE_OPTIONS (see the surefire configuration in the root
 * pom), so it applies to each `node <harness>.cjs` launch without touching the individual tests.
 *
 * Kept (all present in Node 18): Array.prototype.{at,findLast,findLastIndex}, Object.hasOwn, structuredClone,
 * URL.canParse, String.prototype.replaceAll.
 * Removed (added after Node 18):
 *   Node 20  Array/TypedArray prototype toSorted, toReversed, toSpliced (Array only), with;
 *            String.prototype.isWellFormed, toWellFormed
 *   Node 21  Object.groupBy, Map.groupBy; ArrayBuffer.prototype.transfer, transferToFixedLength
 *   Node 22  Promise.withResolvers; Array.fromAsync; the ES2025 Set methods (union, intersection, difference,
 *            symmetricDifference, isSubsetOf, isSupersetOf, isDisjointFrom); the global Iterator and its helpers
 *   Node 24  Promise.try; RegExp.escape; Error.isError
 * Node's own internals use some of these builtins at call time (undici behind fetch calls Promise.withResolvers on
 * Node 26 and String.prototype.toWellFormed on every request), so a builtin is not deleted outright.  Each one becomes
 * an accessor that returns the real function only to a caller whose stack frame is a Node module (node:... or
 * internal/...) and undefined to everyone else, so harness code still gets "X is not a function" while fetch, http and
 * streams keep working on any Node.  The lazy internals are also loaded up front, before the replacement.
 *
 * When the floor is raised, shrink this list to match and update juneau.node.floor and NodeFloorScan_Test.
 */
'use strict';

// Force the lazy internals to compile while the newer builtins still exist.
for (const m of ['assert', 'async_hooks', 'buffer', 'child_process', 'crypto', 'events', 'fs', 'http', 'http2', 'https', 'net',
	'os', 'path', 'perf_hooks', 'querystring', 'readline', 'stream', 'string_decoder', 'timers', 'tls', 'url', 'util', 'vm',
	'worker_threads', 'zlib'])
	try { require(m); } catch (e) { /* module absent on an older Node; the preload must never throw */ } // NOSONAR javascript:S2486 - the preload must never throw
for (const g of ['fetch', 'Request', 'Response', 'Headers', 'FormData', 'WebSocket', 'EventSource', 'MessageChannel', 'BroadcastChannel',
	'TextDecoderStream', 'ReadableStream', 'CompressionStream', 'Blob', 'File', 'URL', 'URLSearchParams', 'AbortController'])
	try { if (g in globalThis) void globalThis[g]; } catch (e) { /* ignore */ } // NOSONAR javascript:S2486,javascript:S3735 - the preload must never throw; void forces the lazy global getter

const TYPED_ARRAY = Object.getPrototypeOf(Int8Array);
const proto = (c) => c?.prototype;

const REMOVED = [
	[Array.prototype, ['toSorted', 'toReversed', 'toSpliced', 'with']],
	[TYPED_ARRAY.prototype, ['toSorted', 'toReversed', 'with']],
	[proto(ArrayBuffer), ['transfer', 'transferToFixedLength']],
	[proto(Set), ['union', 'intersection', 'difference', 'symmetricDifference', 'isSubsetOf', 'isSupersetOf', 'isDisjointFrom']],
	[Object, ['groupBy']],
	[Map, ['groupBy']],
	[Promise, ['withResolvers', 'try']],
	[Array, ['fromAsync']],
	[RegExp, ['escape']],
	[proto(String), ['isWellFormed', 'toWellFormed']],
	[Error, ['isError']],
	[globalThis, ['Iterator']]
];

// Node's own runtime code calls some of these at CALL time (undici's fetch calls Promise.withResolvers on Node 26, and
// String.prototype.{isWellFormed,toWellFormed} on every request), so plain deletion would break fetch itself.  Each
// builtin is therefore replaced by an accessor that hands the real function only to a caller inside Node's own modules
// (a "node:internal/..." stack frame) and answers undefined to everyone else.  User code then sees exactly what it
// sees on the floor Node: "typeof X" is "undefined" and calling it throws "X is not a function".
// Frame 3 is the code that read the property (see the getter in hide).
const fromNode = () => /node:|internal\//.test((new Error('caller probe').stack || '').split('\n')[3] || '');

const hide = (target, name) => {
	const desc = Object.getOwnPropertyDescriptor(target, name);
	if (!desc || !('value' in desc) || !desc.configurable)
		return;  // Absent on this Node already (nothing to hide), or not replaceable.
	const original = desc.value;
	Object.defineProperty(target, name, {
		configurable: true,
		enumerable: false,
		get() { return fromNode() ? original : undefined; },  // Stack: 0 message, 1 fromNode, 2 this getter, 3 the code reading the property.
		set(v) { Object.defineProperty(target, name, { value: v, configurable: true, writable: true, enumerable: false }); }
	});
};

for (const [target, names] of REMOVED)
	for (const name of names)
		try { if (target) hide(target, name); } catch (e) { /* ignore */ } // NOSONAR javascript:S2486 - the preload must never throw
