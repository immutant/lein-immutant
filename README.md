# lein-immutant

A Leiningen plugin for deploying Immutant apps.

## Usage

### Installation

To install for all lein projects, execute:
    
    lein plugin install lein-immutant 0.1.0
    
If you don't want to install it for all lein projects, you can add it as 
a dev dependency in your project.clj:

    ...
    :dependencies [[org.clojure/clojure "1.3.0"]]
    :dev-dependencies [[lein-immutant "0.1.0"]]
    ...
    
### Running it

First, you'll need to define an environment variable called `IMMUTANT_HOME`
pointing to your Immutant distribution. 

* `lein immutant init` - creates a sample immuntant.clj configuration
  file at the root of the project.
  
* `lein immutant deploy` - deploys the current app to the Immutant you 
  specified above. If a map is defined under the `:immutant` key in
  `project.clj`, it will be merged with the deployed descriptor. This
  is useful for setting your `:init` function.

* `lein immutant undeploy` - undeploys the current app from the Immutant 
  you specified above.
  
* `lein immutant run` - launches Immutant. 

## License

Copyright (C) 2011 Red Hat, Inc.

Licensed under the Eclipse Public License v1.0
