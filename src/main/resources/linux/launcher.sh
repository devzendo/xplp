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

sh=sh
# #73162: Ubuntu uses the ancient Bourne shell, which does not implement trap well.
if [ -x /bin/bash ]
then
	sh=/bin/bash
fi
eval exec $sh java ${xplp.linuxjvmargs}-cp $progdir/lib ${xplp.mainclassname}
exit 1
