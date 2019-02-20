#!/usr/bin/env bash

function cloneBidragCucumberBranch() {
  git clone https://$1:$2@github.com/navikt/bidrag-cucumber.git bidrag-cucumber
  STATUS=$?

  if [[ ${STATUS} -eq 0 ]]
    then
    cd bidrag-cucumber
    echo 'pwd:' && pwd
    echo '****** BRANCH ******'
    echo 'BRANCH CHECKOUT: $2......'

    git checkout $2
    exit 0
  fi

  exit ${STATUS}
}
