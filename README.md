# bidrag-jenkins

This is the shared library for automated continuous integration for bidrag-dokument.

Supported build types:
- maven
- node

Summary of workflow of naisPipeline using git branches:
- master
  - the production code which reflects the code in the production environment
  - deploys to preprod-fss in default namespace until first release is done
  - will bump major version (in develop) for each build if being deployed to prod-fss
  - build must be triggered automatically
- develop
  - the head branch used for integration testing and development of new code
  - deploys to prepreod-fss with default namespace
  - will bump major version for each build
  - can be triggered automatically
- feature branches
  - new features being developed
  - deploys to preprod-fss with t0 namespace
  - can be triggered automatically

Descriptions of pipelines:
- mavenPipeline.groovy
  - pipeline for deploying maven artifacts to the "in-house" maven repository
- naisPipeline.groovy
  - pipeline used for building docker images and deploying these images on the "NAIS"
   platform
  - before any code is submitted to the production area, all branches will be deployed
   to the "nais cluster" called pre-prod
  - the master branch will be deployed the to q0 environment under the default namespace
   (kubernetes)
  - the develop branch will be deployed to the q0 environment under the default namespace
   (kubernetes)
  - the feature branches will be dployed to the t0 environment using the namespace t0
