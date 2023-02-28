#!/bin/bash

#mvn clean package
mvn exec:exec -Dexec.executable="java" -Dexec.args="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 -jar functions-framework-invoker-1.0.0-jar-with-dependencies.jar"
