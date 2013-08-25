#!/bin/sh
cd `dirname "$0"`
ant clean
echo "Deleting nbproject/private"
rm -Rf nbproject/private
