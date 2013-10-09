#!/bin/bash

curl -f -L https://raw.github.com/technomancy/leiningen/2.3.3/bin/lein > ./lein
chmod +x ./lein
./lein version
LEIN2_CMD=`pwd`/lein
PRINT_OUT=1 ./lein midje
