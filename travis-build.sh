#!/bin/bash

curl -f -L https://raw.github.com/technomancy/leiningen/2.3.3/bin/lein > ./lein
chmod +x ./lein
./lein version
PRINT_OUT=1 ./lein midje
