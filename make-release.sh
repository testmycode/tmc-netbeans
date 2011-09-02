#!/bin/bash

cd `dirname "$0"`

if [ -z "$1$2" -o "$1" == "-h" -o "$1" == "--help" ]; then
    echo "Usage: $0 TailoringClass VersionQualifier"
    echo
    echo "  e.g. $0 UniversityOfHelsinkiTailoring univ-helsinki"
    echo
    exit 1
fi

TAILORING="$1"
VERSION_QUALIFIER="$2"
VERSION=`cat manifest.mf | grep OpenIDE-Module-Specification-Version | awk '{ print $2 }'`_$VERSION_QUALIFIER

OUTPUT="build/TestMyCode-$VERSION.nbm"

TAILORING_PROPERTIES_FILE="src/fi/helsinki/cs/tmc/tailoring/SelectedTailoring.properties"

echo "defaultTailoring=fi.helsinki.cs.tmc.tailoring.$TAILORING" > $TAILORING_PROPERTIES_FILE

ant nbm
mv "build/TestMyCode.nbm" "$OUTPUT"

echo
echo "Done!"
echo "Created $OUTPUT with $TAILORING"
echo

