# lein-immutant

A Leiningen plugin for deploying Immutant apps.

## Usage

### Installation

#### Leiningen 1

To install for all lein projects, execute:
    
    lein plugin install lein-immutant 0.9.0
    
If you don't want to install it for all lein projects, you can add it as 
a dev dependency in your project.clj:

    ...
    :dependencies [[org.clojure/clojure "1.3.0"]]
    :dev-dependencies [[lein-immutant "0.9.0"]]
    ...
    
#### Leiningen 2

Add it to your `~/.lein/profiles.clj`:

    {:user {:plugins [[lein-immutant "0.9.0"]]}
      
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
  
* `lein immutant archive [--include-dependencies] [path/to/project]` - 
  creates an Immutant archive (suffixed with `.ima`) in the current directory. 
  This archive can be deployed in lieu of a descriptor pointing to the app 
  directory. If the `--include-dependencies` (or `-i`) option is provided, all
  of the app's deps will be included in the archive as well. This task can be 
  run outside of a project dir of the path to the project is provided.
  
* `lein immutant deploy [--archive [--include-dependencies]] [--profiles :p1,p2] [path/to/project]` - 
  deploys the current app to the current Immutant. If passed the `--archive` 
  option, it will deploy an archive of the app instead of a descriptor pointing 
  to the app on disk. This will currently recreate the archive on every deploy. 
  You can pass a comma separated list of lein profiles via the `--profiles :p1,p2`
  option to have them set as the `:lein-profiles` key in the descriptor and
  applied when the app is deployed. This task can be run outside of a project 
  dir of the path to the project is provided.

* `lein immutant undeploy` - undeploys the current app from the current
  Immutant. This task can be run outside of a project dir of the path to 
  the project is provided.
  
* `lein immutant run` - launches the current Immutant. 

#### Using the plugin on Windows

There are two differences when using the plugin on windows:

* `~/.lein/immutant/current' isn't a link to the currently active Immutant installation, 
  but is instead a text file containing the path to that installation. This is to
  work around linking issues on Windows.

* Using `^C` to exit Immutant after calling `lein immutant run` will not work under
  [git-bash](http://msysgit.github.com/) or [cygwin](http://www.cygwin.com/) - it instead
  just detaches the shell from the java process, leaving it running. Using `command.com`
  does not exhibit this problem.
  
## Development

This plugin depends on the [deploy-tools](https://github.com/immutant/deploy-tools/) 
project, so you'll need to checkout and install that first.

## License

Copyright (C) 2011-2012 Red Hat, Inc.

Licensed under the Eclipse Public License v1.0
