#!/usr/bin/env bash
set -euo pipefail

lein uberjar
JAR=$(find "./target/" -maxdepth 1 -name '*standalone.jar' -print | tail -n 1)
cp "$JAR" "./"
zip -u "./${JAR##*/}" $1
