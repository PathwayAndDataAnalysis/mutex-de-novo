#!/bin/env bash

# capture snakemake/ directory location in a variable
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SNAKEDIR="$(readlink -f $DIR/../)/"

# identify location where snakemake log will be stored
SNAKELOG=${SNAKEDIR}snakelogs/repro/nosched_snakelog.out
echo "log filepath:"
echo ${SNAKELOG}

# make log directory if it doesn't already exist
mkdir -p ${SNAKEDIR}snakelogs/repro/

# cd into directory containing snakefiles
cd ${SNAKEDIR}snakefiles/

# launch snakemake and run it in the background
# see: https://snakemake.readthedocs.io/en/stable/executable.html
nohup snakemake --nolock --rerun-incomplete -s Snakefile_repro_nosched > ${SNAKELOG} &
