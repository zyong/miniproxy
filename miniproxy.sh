mvn compile
mvn exec:java@inprocess -Dexec.args="$*" $JAVA_OPTS
