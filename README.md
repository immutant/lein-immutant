# lein-immutant

A Leiningen plugin for deploying Immutant apps.

## Usage

### Installation

This isn't (yet) published to clojars, so you'll need to build it first. 
To do so, checkout this repo and `lein install`.

To install for all lein projects, execute:
    
    lein plugin install lein-immutant 1.0.0-SNAPSHOT
    
If you don't want to install it for all lein projects, you can add it as 
a dev dependency in your project.clj:

    ...
    :dependencies [[org.clojure/clojure "1.3.0"]]
    :dev-dependencies [[lein-immutant "1.0.0-SNAPSHOT"]]
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
  
* `lein immutant run` - launches Immutant. Not yet implemented.

## License

Copyright (C) 2011 Red Hat, Inc.

License TBD.
