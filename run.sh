#!/usr/bin/env bash
# ResourceFlow launcher for macOS / Linux.  Usage: ./run.sh            (desktop app)
#                                                  ./run.sh --console  (text menu)
cd "$(dirname "$0")"

# Major version of a java binary: "1.8.0_472" -> 8, "21.0.4" -> 21, "25" -> 25
java_major() {
    "$1" -version 2>&1 | awk -F'"' '/version/ {split($2, v, "."); print (v[1] == "1" ? v[2] : v[1]); exit}'
}

# Use the java on PATH if it is 21+, otherwise look for a newer JDK
# (macOS java_home, then Homebrew) - the default java on many Macs is still 8.
JAVA=""
if command -v java >/dev/null 2>&1 && [ "$(java_major java)" -ge 21 ] 2>/dev/null; then
    JAVA="java"
else
    for home in "$(/usr/libexec/java_home -v 21+ 2>/dev/null)" \
                /opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home \
                /usr/local/opt/openjdk/libexec/openjdk.jdk/Contents/Home; do
        if [ -n "$home" ] && [ -x "$home/bin/java" ] && [ "$(java_major "$home/bin/java")" -ge 21 ] 2>/dev/null; then
            JAVA="$home/bin/java"
            export JAVA_HOME="$home"
            break
        fi
    done
fi

if [ -z "$JAVA" ]; then
    echo "ERROR: Java 21 or later not found. Please install it (https://adoptium.net/)."
    exit 1
fi

JAR="target/resourceflow-1.0.0.jar"
if [ ! -f "$JAR" ]; then
    echo "JAR not found, building it first..."
    ./.tools/apache-maven-3.9.9/bin/mvn -q package -DskipTests || exit 1
fi

exec "$JAVA" -jar "$JAR" "$@"
