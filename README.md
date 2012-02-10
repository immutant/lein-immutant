# lein-immutant

A Leiningen plugin for deploying Immutant apps.

## Usage

### Installation

To install for all lein projects, execute:
    
    lein plugin install lein-immutant 0.4.0
    
If you don't want to install it for all lein projects, you can add it as 
a dev dependency in your project.clj:

    ...
    :dependencies [[org.clojure/clojure "1.3.0"]]
    :dev-dependencies [[lein-immutant "0.4.0"]]
    ...
    
### Running it

* `lein immutant install [version [install-dir]]` - downloads and installs
   Immutant for you. By default, it will download the latest incremental
   build and put it in `~/.lein/immutant/releases/`. You can override the
   version (which currently must be an incremental build number from 
   http://immutant.org/builds/) and the install directory. Wherever it gets
   installed, the most recently installed version will be linked from
   `~/.lein/immutant/current`. If this link is present (and points to a 
   valid Immutant install), you won't need to set `$IMMUTANT_HOME`

* `lein immutant overlay [feature-set [version]]` - downloads and overlays a
  feature set onto the currenty installed Immutant. If it can't find
  an Immutant install (either via the `current` link or `$IMMUTANT_HOME`), 
  it will download and install the latest incremental build first. Currently,
  the only supported feature set is 'torquebox'. The version defaults to the
  latest incremental, but can also be any recent build number from
  http://torquebox.org/2x/builds/.

* `lein immutant env [key]` - prints out information about the Immutant
  environment. It currently only displays the path to the current Immutant,
  and the path to JBoss.
  
* `lein immutant new project-name` - calls `lein new project-name` for you,
   the calls `lein immutant init`.

* `lein immutant init` - creates a sample immuntant.clj configuration
  file at the root of the current project.
  
* `lein immutant archive` - creates an Immutant archive (suffixed with `.ima`)
  in the app directory. This archive can be deployed in lieu of a descriptor
  pointing to the app directory.
  
* `lein immutant deploy` - deploys the current app to the current Immutant. 
  If a map is defined under the `:immutant` key in  `project.clj`, it will 
  be merged with the deployed descriptor. This is useful for setting your 
  `:init` function. If passed the `--archive` option, it will deploy an
  archive of the app instead of a descriptor pointing to the app on
  disk. If an archive does not yet exist, one will be created.

* `lein immutant undeploy` - undeploys the current app from the current
  Immutant.
  
* `lein immutant run` - launches the current Immutant. 

## License

Copyright (C) 2011 Red Hat, Inc.

Licensed under the Eclipse Public License v1.0
