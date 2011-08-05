#!/bin/bash

# script to reinstall myberkeley on calcentral-dev/calcentral-qa, while preserving content repository

if [ -z "$1" ]; then
    echo "Usage: $0 source_root logfile"
    exit;
fi

SRC_LOC=$1

INPUT_FILE="$SRC_LOC/.build.cf"
if [ -f $INPUT_FILE ]; then
  SLING_PASSWORD=`awk -F"=" '/APPLICATION_PASSWORD/ {print $2}' $INPUT_FILE`
else
  SLING_PASSWORD='admin'
fi

LOG=$2
if [ -z "$2" ]; then
    LOG=/dev/null
fi
LOGIT="tee -a $LOG"

echo "=========================================" | $LOGIT
echo "`date`: Update started" | $LOGIT

echo | $LOGIT

cd $SRC_LOC/myberkeley
echo "`date`: Stopping sling..." | $LOGIT
mvn -B -e -Dsling.stop -P runner verify >>$LOG 2>&1 | $LOGIT
echo "`date`: Cleaning sling directories..." | $LOGIT
mvn -B -e -P runner -Dsling.purge clean >>$LOG 2>&1 | $LOGIT
rm -rf ~/.m2/repository/edu/berkeley

echo "`date`: Fetching new sources for myberkeley..." | $LOGIT
git pull >>$LOG 2>&1
echo "Last commit:" | $LOGIT
git log -1 | $LOGIT
echo | $LOGIT
echo "------------------------------------------" | $LOGIT

echo "`date`: Fetching new sources for 3akai-ux..." | $LOGIT
cd ../3akai-ux
git pull >>$LOG 2>&1
echo "Last commit:" | $LOGIT
git log -1 | $LOGIT
echo | $LOGIT
echo "------------------------------------------" | $LOGIT

cd ../myberkeley

echo "`date`: Doing clean install..." | $LOGIT
mvn -B -e clean install >>$LOG 2>&1

echo "`date`: Starting sling..." | $LOGIT
mvn -B -e -Dsling.start -P runner verify >>$LOG 2>&1

# wait 2 minutes so sling can get going
sleep 120;

echo "`date`: Redeploying UX..." | $LOGIT
cd ../3akai-ux
mvn clean install -P sakai-release
mvn -B -e -Dsling.user=admin -Dsling.password=$SLING_PASSWORD org.apache.sling:maven-sling-plugin:install-file \
  -Dsling.file=./target/org.sakaiproject.nakamura.uxloader-myberkeley-0.7-SNAPSHOT.jar

echo | $LOGIT
echo "`date`: Reinstall complete." | $LOGIT

