#!/bin/env bash

# To initiate a parallelized reproduction of this study using the slurm scheduler,
# run the command "source submit_slurm_repro.sh" 

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SNAKEDIR="$(readlink -f $DIR/../)/"
SUBMITDIR=${SNAKEDIR}submitters/
LOGDIR=${SNAKEDIR}snakelogs/repro/

echo LOGDIR is $LOGDIR

mkdir -p $LOGDIR

cd ${SUBMITDIR}
sbatch --time=3-01:00:00 --job-name=subber_ASD_mutex_repro --output=${LOGDIR}submitter_repro_%j.out --error=${LOGDIR}submitter_repro_%j.err --partition=long_jobs slurm_repro.sh
