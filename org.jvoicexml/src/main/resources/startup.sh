#!/bin/bash
#
# Normally, editing this script should not be required.
# Only case is to set up JAVA_HOME if it's not allready defined.
#
# To specify an alternative jvm, edit and uncomment the following line
# JAVA_HOME=/usr/lib/java

JAVA_CMD="java"
if [ -n $JAVA_HOME ] ; then
    _TMP="$JAVA_HOME/bin/java"
    if [ -f "$_TMP" ] ; then
        if [ -x "$_TMP" ] ; then
            JAVA_CMD="$_TMP"
        else
            echo "Warning: $_TMP is not executable"
        fi
    else
        echo "Warning: $_TMP does not exist"
    fi
fi
if ! which "$JAVA_CMD" >/dev/null ; then
    echo "Error: No java environment found"
    exit 1
fi

#
# Resolve the location of the JVoiceXML installation.
# This includes resolving any symlinks.
PRG=$0
while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '^.*-> \(.*\)$' 2>/dev/null`
    if expr "$link" : '^/' 2> /dev/null >/dev/null; then
        PRG="$link"
    else
        PRG="`dirname "$PRG"`/$link"
    fi
done

JVOICEXML_BIN=`dirname "$PRG"`

# absolutize dir
oldpwd=`pwd`
cd "${JVOICEXML_BIN}"; JVOICEXML_BIN=`pwd`
cd "${oldpwd}"; unset oldpwd

JVOICEXML_HOME=`dirname "${JVOICEXML_BIN}"`

VMOPTIONS=-mx256m
VMOPTIONS="${VMOPTIONS} -Djava.util.logging.config.file=${JVOICEXML_HOME}/config/logging.properties"
VMOPTIONS="${VMOPTIONS} -Djava.security.policy=config/jvoicexml.policy"

# GJTAPI settings
VMOPTIONS="${VMOPTIONS} -Dgjtapi.sip.properties=/gjtapi-provider.properties"

# Enable RTP streaming via jlibrtp
VMOPTIONS="${VMOPTIONS} -Djava.protocol.handler.pkgs=org.jlibrtp.protocols"

# Disallow FreeTTS to add jars to the classpath
VMOPTIONS="${VMOPTIONS} -Dfreetts.nocpexpansion=true"
VMOPTIONS="${VMOPTIONS} -Djava.library.path=${JVOICEXML_HOME}/lib"

JVOICEXML_LIB="${JVOICEXML_HOME}/lib"

LOCAL_CLASSPATH="$JVOICEXML_HOME/config"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/log4j-1.2.17.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/spring-core-4.2.0.RELEASE.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/spring-beans-4.2.0.RELEASE.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/spring-context-4.2.0.RELEASE.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/spring-expression-4.2.0.RELEASE.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/commons-pool-1.5.5.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/commons-logging-1.1.1.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/commons-codec-1.4.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/commons-lang3-3.3.2.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/commons-codec-1.4.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/httpcore-4.2.3.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/httpcore-nio-4.2.3.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/httpcore-ab-4.2.3.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/httpclient-4.2.3.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/httpclient-cache-4.2.3.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/fluent-hc-4.2.3.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/httpmime-4.2.3.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/chardet.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/jmf.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/jlibrtp.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/jetty-http-9.2.2.v20140723.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/jetty-io-9.2.2.v20140723.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/jetty-server-9.2.2.v20140723.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/jetty-util-9.2.2.v20140723.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/servlet-api-3.1.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/org.jvoicexml.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/org.jvoicexml.xml.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/org.jvoicexml.config.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/org.jvoicexml.jndi.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/org.jvoicexml.srgs.jar"
LOCAL_CLASSPATH="${LOCAL_CLASSPATH}:${JVOICEXML_LIB}/org.jvoicexml.client.jar"

cd ${JVOICEXML_HOME}

$JAVA_CMD $VMOPTIONS -classpath $LOCAL_CLASSPATH org.jvoicexml.startup.Startup

