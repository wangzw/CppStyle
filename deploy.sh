#!/usr/bin/env bash
BRANCH=gh-pages
TARGET_REPO=wangzw/cppstyle
OUTPUT_FOLDER=update/target/site
SITE_DIR=$TRAVIS_BRANCH

echo -e "Testing travis-encrypt"
echo -e "$VARNAME"

if [ "$TRAVIS_PULL_REQUEST" == "false" ]; then
    echo -e "Starting to deploy to Github Pages\n"
    if [ "$TRAVIS" == "true" ]; then
        git config --global user.email "travis@travis-ci.org"
        git config --global user.name "Travis"
    fi
    #using token clone gh-pages branch
    git clone --quiet --branch=$BRANCH https://${GH_TOKEN}@github.com/$TARGET_REPO built_website > /dev/null
    #go into directory and copy data we're interested in to that directory
    cd built_website

    if [ "$TRAVIS_BRANCH" == "master" ]; then
        SITE_DIR=update
    fi

    rm -rf $SITE_DIR
    mkdir -p $SITE_DIR
    cd $SITE_DIR
    rsync -rv --exclude=.git  ../../$OUTPUT_FOLDER/* .

    #add, commit and push files
    git add --all -f .
    git commit -m "Travis build $TRAVIS_BUILD_NUMBER pushed to Github Pages"
    git push -fq origin $BRANCH > /dev/null
    echo -e "Deploy completed\n"
fi
