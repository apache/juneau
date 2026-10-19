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
 *            String.prototype.isWellFormed, toWellFormed (guarded rather than deleted, see below)
 *   Node 21  Object.groupBy, Map.groupBy; ArrayBuffer.prototype.transfer, transferToFixedLength
 *   Node 22  Promise.withResolvers; Array.fromAsync; the ES2025 Set methods (union, intersection, difference,
 *            symmetricDifference, isSubsetOf, isSupersetOf, isDisjointFrom); the global Iterator and its helpers
 *   Node 24  Promise.try; RegExp.escape; Error.isError
 * Node's own lazily loaded internals (undici behind fetch, http, ...) use these builtins, so they are loaded before
 * the deletion; otherwise the first fetch in a harness would trip over the removal instead of the harness's own code.
 *
 * When the floor is raised, shrink this list to match and update juneau.node.floor and NodeFloorScan_Test.
 */
'use strict';

// Force the lazy internals to compile while the newer builtins still exist.
for (const m of ['assert', 'async_hooks', 'buffer', 'child_process', 'crypto', 'events', 'fs', 'http', 'http2', 'https', 'net',
	'os', 'path', 'perf_hooks', 'querystring', 'readline', 'stream', 'string_decoder', 'timers', 'tls', 'url', 'util', 'vm',
	'worker_threads', 'zlib'])
	try { require(m); } catch (e) { /* module absent on an older Node; the preload must never throw */ }
for (const g of ['fetch', 'Request', 'Response', 'Headers', 'FormData', 'WebSocket', 'EventSource', 'MessageChannel', 'BroadcastChannel',
	'TextDecoderStream', 'ReadableStream', 'CompressionStream', 'Blob', 'File', 'URL', 'URLSearchParams', 'AbortController'])
	try { if (g in globalThis) void globalThis[g]; } catch (e) { /* ignore */ }

const TYPED_ARRAY = Object.getPrototypeOf(Int8Array);
const proto = (c) => c && c.prototype;

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
	[Error, ['isError']],
	[globalThis, ['Iterator']]
];

// Node's own runtime code (undici's string conversion behind fetch) calls String.prototype.{isWellFormed,toWellFormed}
// on every request, so deleting them would break fetch itself.  They stay, but throw "is not a function" to any caller
// outside Node's own modules, which is the observable failure on an older Node.
for (const name of ['isWellFormed', 'toWellFormed']) {
	const original = String.prototype[name];
	if (typeof original !== 'function')
		continue;  // Absent on this Node already, so there is nothing to guard.
	const guarded = {
		[name](...args) {
			const stack = new Error().stack.split('\n')[2] || '';
			if (!/\(?node:|^\s*at node:|\(?internal\//.test(stack))
				throw new TypeError(`${this}.${name} is not a function (removed by node-floor-preload)`);
			return original.apply(this, args);
		}
	}[name];
	Object.defineProperty(String.prototype, name, { value: guarded, configurable: true, writable: true, enumerable: false });
}

for (const [target, names] of REMOVED)
	for (const name of names)
		try { if (target) delete target[name]; } catch (e) { /* ignore */ }
