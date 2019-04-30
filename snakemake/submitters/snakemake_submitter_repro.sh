#!/bin/env bash

#SBATCH --time=3-01:00:00
#SBATCH --job-name=subber_autism_mutex_repro
#SBATCH --output=/home/groups/precepts/manningh/codebases/mutex-de-novo/snakemake/snakelogs/repro/submitter_repro_%j.out
#SBATCH --error=/home/groups/precepts/manningh/codebases/mutex-de-novo/snakemake/snakelogs/repro/submitter_repro_%j.err
#SBATCH --partition=long_jobs

# move to snakemake dir
SNAKEDIR=/home/groups/precepts/manningh/codebases/mutex-de-novo/snakemake/

# make log directories
mkdir ${SNAKEDIR}snakelogs/
mkdir ${SNAKEDIR}snakelogs/repro/

# cd into directory containing snakefiles
cd ${SNAKEDIR}snakefiles/

# submit snakemake job
# see: https://snakemake.readthedocs.io/en/stable/executable.html
snakemake -j 200 --nolock --rerun-incomplete --latency-wait 259200 --cluster-config ../cluster_jsons/repro.json --cluster "sbatch -p {cluster.partition} -n {cluster.nodes} -t {cluster.time} -J {cluster.J} -o {cluster.o} -e {cluster.e} --mem {cluster.mem}" -s Snakefile_repro

