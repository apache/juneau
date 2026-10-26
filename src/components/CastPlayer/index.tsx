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

import {useEffect, useRef, type ReactNode} from 'react';
import BrowserOnly from '@docusaurus/BrowserOnly';
import useBaseUrl from '@docusaurus/useBaseUrl';

type Props = {
  /** Site-relative path of the `.cast` file, e.g. `/casts/demo.cast`. */
  src: string;
  /** Time to show before playback starts, e.g. `npt:0:12`. */
  poster?: string;
  /** Playback speed multiplier. */
  speed?: number;
  /** Longest pause, in seconds, that plays back as is. */
  idleTimeLimit?: number;
};

function Player({src, poster, speed, idleTimeLimit}: Props): ReactNode {
  const host = useRef<HTMLDivElement>(null);
  const url = useBaseUrl(src);
  useEffect(() => {
    let player: {dispose: () => void} | undefined;
    let cancelled = false;
    // The player touches `window`, so it is loaded only once the page is in a browser.
    void Promise.all([import('asciinema-player'), import('asciinema-player/dist/bundle/asciinema-player.css')]).then(
      ([lib]) => {
        if (cancelled || !host.current) return;
        player = lib.create(url, host.current, {poster, speed, idleTimeLimit, fit: 'width'});
      },
    );
    return () => {
      cancelled = true;
      player?.dispose();
    };
  }, [url, poster, speed, idleTimeLimit]);
  return <div ref={host} />;
}

export default function CastPlayer(props: Props): ReactNode {
  return <BrowserOnly fallback={<p>Loading the recording...</p>}>{() => <Player {...props} />}</BrowserOnly>;
}
