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
"""PTY fixture child: colour, a \\r progress counter, cursor movement and the alternate screen, then exit 3."""

import sys

w = sys.stdout.write
w("\x1b[1;31mred\x1b[0m plain\n")
for i in range(3):
    w(f"\rcount {i}")
w("\n")
w("\x1b[2Aup\x1b[2B\n")
w("\x1b[?1049halt\x1b[?1049l")
w("done\n")
sys.stdout.flush()
sys.exit(3)
