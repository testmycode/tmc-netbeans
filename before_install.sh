#!/bin/sh
# Installs tmc-core

git clone https://github.com/rage/tmc-core.git
mvn clean install -U -f tmc-core/pom.xml
