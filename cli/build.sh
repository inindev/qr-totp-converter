#!/bin/sh
#
# Copyright 2025, John Clark <inindev@gmail.com>. All rights reserved.
# Licensed under the Apache License, Version 2.0. See LICENSE file in the project root for full license information.
#

set -e

cd "$(dirname "$(realpath "$0")")"

ZXING_DIR='../zxing-totp-qr'
ZXING_JAR='zxing-totp-qr.jar'
QRTOTP_JAR="qr-totp-converter.jar"

echo 'cleaning...'
rm -fv $ZXING_JAR $QRTOTP_JAR
rm -fv totp2qr.sh qr2totp.sh

echo 'fetching zxing jar file...'
sh "$ZXING_DIR/make_jar.sh"
cp -v "$ZXING_DIR/target/$ZXING_JAR" .

echo "building $QRTOTP_JAR..."
kotlinc ./QrTotpConverter.kt -cp ./$ZXING_JAR -include-runtime -d ./$QRTOTP_JAR

echo "java -cp '$ZXING_JAR:$QRTOTP_JAR' QrTotpConverterKt encode \$@" > totp2qr.sh
echo "java -cp '$ZXING_JAR:$QRTOTP_JAR' QrTotpConverterKt decode \$@" > qr2totp.sh

echo 'build complete'
ls -l totp2qr.sh qr2totp.sh
