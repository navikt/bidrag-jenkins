#!/usr/bin/env bash

git clone https://$1:$2@github.com/navikt/bidrag-cucumber.git bidrag-cucumber
STATUS=$?

if [[ ${STATUS} -eq 0 ]]
  then
  CUCUMBER_BRANCH=$2

  cd bidrag-cucumber
  echo 'pwd:' && pwd
  echo '****** BRANCH ******'
  echo 'BRANCH CHECKOUT: '${CUCUMBER_BRANCH}

  git checkout ${CUCUMBER_BRANCH}
  exit 0
fi

exit ${STATUS}
