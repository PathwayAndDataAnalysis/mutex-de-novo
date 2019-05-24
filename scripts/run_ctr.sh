#!/bin/env bash

#SBATCH --account=compbio
#SBATCH --time=10:00:00
#SBATCH --nodes=1
#SBATCH --job-name=asd_repro1
#SBATCH --output=/home/groups/precepts/manningh/codebases/mutex-de-novo/sequentiallogs/repro1_subber%j.out
#SBATCH --error=/home/groups/precepts/manningh/codebases/mutex-de-novo/sequentiallogs/repro1_subber%j.err
#SBATCH --partition=exacloud

cd /home/groups/precepts/manningh/codebases/mutex-de-novo/scripts

# activate conda environment (probably not necessary) and submit job

source ~/.bashrc
conda activate hbasiq
source commands-to-reproduce.sh
