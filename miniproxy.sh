mvn exec:java@inprocess -Dmain.args="$*" $JAVA_OPTS
if [ $? != 0 ]; then
    exit $?;
fi
if [ -f out/artifacts/miniproxy_jar/miniproxy.jar ]
then
  java -jar out/artifacts/miniproxy_jar/miniproxy.jar $*

fi