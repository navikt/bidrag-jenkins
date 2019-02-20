#!/usr/bin/env bash

git clone https://$1:$2@github.com/navikt/bidrag-cucumber.git bidrag-cucumber
STATUS=$?

if [[ ${STATUS} -eq 0 ]]
  then
  CUCUMBER_BRANCH=$3

  cd bidrag-cucumber
  echo 'cloned bidrag-cucumber master to: '${PWD}
  echo 'checkout cucumber branch "'${CUCUMBER_BRANCH}'" for integration testing'

  git checkout ${CUCUMBER_BRANCH}
  exit 0
fi

exit ${STATUS}
