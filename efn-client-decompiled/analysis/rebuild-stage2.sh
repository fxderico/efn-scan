#!/usr/bin/env bash
# Reproduces stage2/src/main/java from the original jar. Static only: nothing from the jar is run.
#   analysis/rebuild-stage2.sh <efn-client-1.2.0.jar> <work-dir>
# Needs: python3, java 21, and in <work-dir>/tools: vineflower.jar (1.11.1),
#        asm.jar (9.7.1), javaparser-core.jar (3.26.2)  -- all from Maven Central.
set -euo pipefail
JAR=$(realpath "$1"); W=$(realpath "$2"); HERE=$(cd "$(dirname "$0")" && pwd); PROJ=$(dirname "$HERE")
T=$W/tools
mkdir -p "$W/payload" && rm -rf "$W/payload"/* "$W/decompiled" "$W/folded"
# 1. pull the encrypted resource out of the jar and decrypt it -> p.jar
unzip -p "$JAR" assets/efn/data/runtime.dat > "$W/payload/runtime.dat"
python3 -I "$HERE/decrypt_runtime.py" "$W/payload/runtime.dat" "$W/payload/p.jar"
# 2. drop the obfuscator's bogus LocalVariableTable / debug attributes
java -cp "$T/asm.jar" "$HERE/StripDebug.java" "$W/payload/p.jar" "$W/payload/p-nodebug.jar"
# 3. decompile
mkdir -p "$W/decompiled"
java -jar "$T/vineflower.jar" -dgs=1 -asc=1 -rsy=1 -lit=1 -log=WARN "$W/payload/p-nodebug.jar" "$W/decompiled"
# 4. fold obfuscated strings, strip opaque predicates, rename members
java -cp "$T/javaparser-core.jar" "$HERE/StringFolder.java" "$W/decompiled" "$W/folded" "$HERE/rename-map.txt"
# 5. one decompiler type-inference fix (Vineflower types the plain/TLS socket as Object)
sed -i 's/^        Object var12;$/        Socket var12;/' "$W/folded/net/efn/loader/HttpTransport.java"
rm -rf "$PROJ/stage2/src/main/java" && mkdir -p "$PROJ/stage2/src/main/java"
cp -r "$W/folded/." "$PROJ/stage2/src/main/java/"
echo "stage2 sources written to $PROJ/stage2/src/main/java"
