#!/bin/sh

SCRIPT=$(find . -type f -name mobile-token-exchange)
exec $SCRIPT \
  $HMRC_CONFIG
