#!/usr/bin/env bash 

ROOT=`cd "\`dirname \"$0\"\`";pwd`

OLD=$1
NEW=$2

[ -z "$OLD" ] && echo "current version is not specified" && exit 1
[ -z "$NEW" ] && echo "target version is not specified" && exit 1

echo "change version from $OLD to $NEW"

case "`uname -s`" in
    Darwin)
        find $ROOT -name "*.xml" -or -name "*.MF" | grep -v metadata | xargs sed -i "" -e "s|$OLD|$NEW|g"
    ;;

    *)
        find $ROOT -name "*.xml" -or -name "*.MF" | grep -v metadata | xargs sed -i -e "s|$OLD|$NEW|g"
    ;;
esac












