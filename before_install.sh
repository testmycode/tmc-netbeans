#!/bin/sh
# Installs tmc-core

mvn clean install -U -f maven-wrapper/pom.xml
git clone https://github.com/rage/tmc-core.git
mvn clean install -U -f tmc-core/pom.xml
