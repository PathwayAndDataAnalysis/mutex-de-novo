#!/bin/env bash

# capture snakemake/ dir location in a variable
#DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
#SNAKEDIR="$(readlink -f $DIR/../)/"

echo SLURM_SUBMIT_DIR is $SLURM_SUBMIT_DIR
SNAKEDIR=${SLURM_SUBMIT_DIR}/../

# make log directory if it doesn't already exist
mkdir -p ${SNAKEDIR}snakelogs/repro/

# cd into directory containing snakefiles
cd ${SNAKEDIR}snakefiles/

# submit snakemake job
# see: https://snakemake.readthedocs.io/en/stable/executable.html
snakemake -j 100 --nolock --rerun-incomplete --cluster-config ../cluster_jsons/slurm_repro.json --cluster "sbatch -A compbio -p {cluster.partition} -n {cluster.nodes} -t {cluster.time} -J {cluster.J} -o {cluster.o} -e {cluster.e} --mem {cluster.mem}" -s Snakefile_repro

