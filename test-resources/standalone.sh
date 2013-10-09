#!/bin/sh
echo "called" $* 
echo "to stdout"
echo "to stderr" 1>&2

sleep .5
