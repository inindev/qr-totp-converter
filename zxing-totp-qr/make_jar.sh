#!/bin/sh
#
# Copyright 2025, John Clark <inindev@gmail.com>. All rights reserved.
# Licensed under the Apache License, Version 2.0. See LICENSE file in the project root for full license information.
#

set -e

cd "$(dirname "$(realpath "$0")")"

echo 'cleaning...'
rm -rf ./target

echo 'compiling...'
javac -d ./target/classes $(find ./src -name "*.java")

echo 'packaging...'
jar cf ./target/zxing-totp-qr.jar -C ./target/classes .

echo 'build complete'
ls -l "$(realpath ./target/zxing-totp-qr.jar)"
