# lein-immutant [![Build Status](https://secure.travis-ci.org/immutant/lein-immutant.png)](http://travis-ci.org/immutant/lein-immutant)

A Leiningen plugin for deploying Immutant apps.

## Usage

### Installation
    
As of version 0.15.0, the plugin only supports leiningen 2.0.0 and up.
To use it, add it to your `~/.lein/profiles.clj`:

    {:user {:plugins [[lein-immutant "0.15.1"]]}}
      
### Using it

The plugin provides several subtasks for installing, deploying to, and
running an Immutant. All of the subtasks are namespaced under the
`immutant` task. You can use `lein help immutant` to see a full list,
and use `lein help immutant SUBTASK` to get more detailed help for a 
specific subtask. 

The subtasks provided by the plugin are:

* lein immutant install [version [install-dir]] - downloads and
  installs Immutant for you. By default, it will download the latest
  incremental build and put it in `~/.lein/immutant/releases/`. You
  can override the version (which must be an incremental build number
  from http://immutant.org/builds/ or a released version) and the
  install directory. Wherever it gets installed, the most recently
  installed version will be linked from
  `~/.lein/immutant/current`. If this link is present (and points to
  a valid Immutant install), you won't need to set `$IMMUTANT_HOME`

* lein immutant overlay [feature-set [version]] - downloads and
  overlays a feature set onto the currenty installed Immutant. If it
  can't find an Immutant install (either via the `current` link or
  `$IMMUTANT_HOME`), it will download and install the latest
  incremental build first. Currently, the only supported feature set
  is 'torquebox'. The version defaults to the latest incremental, but
  can also be any recent build number from
  http://torquebox.org/2x/builds/.

* lein immutant env [key] - prints out information about the
  Immutant environment. It currently only displays the path to the
  current Immutant, and the path to JBoss.
  
* lein immutant new project-name - calls `lein new project-name` for
   you, the calls `lein immutant init`.

* lein immutant init - creates a sample `immutant.init` namespace
  beneath `src/`
  
* lein immutant archive [--include-dependencies] [--name name]
                         [path/to/project] - 
  creates an Immutant archive (suffixed with `.ima`) in the current
  directory.  By default, the archive file will be named after the
  project name in project.clj.  This can be overridden via the
  `--name` (or `-n`) option.  This archive can be deployed in lieu of
  a descriptor pointing to the app directory. If the
  `--include-dependencies` (or `-i`) option is provided, all of the
  application's dependencies will be included in the archive as
  well. This task can be run outside of a project dir of the path to
  the project is provided.
  
* lein immutant deploy [--archive [--include-dependencies]] [--name name] 
                       [--context-path path] [--virtual-host host] 
                       [path/to/project] - 
  deploys the current app to the current Immutant. If passed the
  `--archive` option, it will deploy an archive of the app instead of
  a descriptor pointing to the app on disk. This will currently
  recreate the archive on every deploy.  By default, the deployment
  will be named after the project name in project.clj.  This can be
  overridden via the `--name` (or `-n`) option.  

  Any profiles that are active (via with-profile) will be captured in
  the `:lein-profiles` key in the descriptor and applied when the app is
  deployed.

  You can override the default context-path (based off of the
  deployment name) and virtual-host with the `--context-path` and
  `--virtual-host` options, respectively. This task can be run outside
  of a project dir of the path to the project is provided.

* lein immutant undeploy [--name name] [path/to/project] - undeploys
  the current app from the current Immutant. If the `--name` option
  was used to deploy the app, you'll need to pass the same name to
  undeploy as well. This task can be run outside of a project dir of
  the path to the project is provided.
  
* lein immutant run - launches the current Immutant. 

* lein immutant test [--name name] [--dir test] [--port 7888]
  [path/to/project] - runs the current Immutant, if necessary,
  deploys the project to it, runs all tests found beneath the `test/`
  directory, undeploys the app, and then shuts down the Immutant it
  started. The `--port` option specifies the nREPL service port
  through which the tests are invoked inside the running Immutant.
  This is a very simple way to automate your integration testing on a
  [CI](http://en.wikipedia.org/wiki/Continuous_integration) host.
  
#### Using the plugin on Windows

There are two differences when using the plugin on windows:

* `~/.lein/immutant/current` isn't a link to the currently active
  Immutant installation, but is instead a text file containing the
  path to that installation. This is to work around linking issues on
  Windows.

* Using `^C` to exit Immutant after calling `lein immutant run` will
  not work under [git-bash](http://msysgit.github.com/) or
  [cygwin](http://www.cygwin.com/) - it instead just detaches the
  shell from the java process, leaving it running. Using `command.com`
  does not exhibit this problem.
  
## Development

The plugin has a mediocre midje test suite. You can run it via:

    lein midje

## License

Copyright (C) 2011-2013 Red Hat, Inc.

Licensed under the Eclipse Public License v1.0
