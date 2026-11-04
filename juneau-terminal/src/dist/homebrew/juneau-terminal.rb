# ***************************************************************************************************************************
# * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
# * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
# * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
# * with the License.  You may obtain a copy of the License at
# *
# *  http://www.apache.org/licenses/LICENSE-2.0
# *
# * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
# * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# * specific language governing permissions and limitations under the License.
# ***************************************************************************************************************************
# Template for Formula/juneau-terminal.rb in the apache/homebrew-juneau tap.  It installs the voted .pyz itself (no pip,
# no resources).  release.py's update-brew-formula step rewrites the url, mirror and sha256 lines for each release;
# @VERSION@ and @SHA256@ are filled in by hand once, when the tap is seeded.
class JuneauTerminal < Formula
  desc "Terminal-protocol progress for build tools, and tmux pane helpers for long runs"
  homepage "https://juneau.apache.org/"
  url "https://downloads.apache.org/juneau/@VERSION@/apache-juneau-terminal-@VERSION@.pyz"
  mirror "https://archive.apache.org/dist/juneau/@VERSION@/apache-juneau-terminal-@VERSION@.pyz"
  sha256 "@SHA256@"
  license "Apache-2.0"

  depends_on "python@3.13"

  def install
    # A download keeps no mode bits, so the wrapper runs the archive with python rather than through its shebang.  The
    # versioned file name lets the toolkit extract its bin/ scripts under ~/.cache/juneau-terminal/bin/<version>/.
    libexec.install Dir["*"].first => "juneau-terminal-#{version}.pyz"
    (bin/"juneau-terminal").write <<~SH
      #!/bin/bash
      exec "#{Formula["python@3.13"].opt_bin}/python3.13" "#{libexec}/juneau-terminal-#{version}.pyz" "$@"
    SH
  end

  test do
    assert_match "juneau-terminal #{version} (terminal-protocol v", shell_output("#{bin}/juneau-terminal --version")
  end
end
