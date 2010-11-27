#!/bin/sh
#
# resolve symlinks to launch script
#

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

progdir=`dirname "$PRG"`
APPNAME=`basename "$0"`

exec java ${xplp.linuxjvmargs}-cp ${xplp.linuxclasspatharray} -Djava.library.path=$progdir/../lib ${xplp.mainclassname} $@
