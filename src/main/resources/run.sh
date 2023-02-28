#!/bin/bash

#mvn clean package
mvn exec:exec -Dexec.executable="java" -Dexec.args="-jar functions-framework-invoker-1.0.0-jar-with-dependencies.jar"
