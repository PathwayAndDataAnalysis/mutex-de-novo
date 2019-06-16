# Using Snakemake to reproduce our analyses

This readme will guide you through reproducing the results in [our 2019 manuscript](https://www.biorxiv.org/content/10.1101/653527v1). This manuscript currently undergoing peer review. In the meantime, it is available on bioRxiv in its original form. 

The reproducibility pipeline is available in three flavors: a sequential reproduction, a parallelized workflow that relies on the slurm workload manager, and a parallelized workflow that relies on the PBS scheduler. Each style requires that you have copied and built the mutex-de-novo software as described [here](https://github.com/PathwayAndDataAnalysis/mutex-de-novo).

Please reach out if you encounter any issues with these workflows or our analyses, or if you are using a scheduler that is not included here. We are happy to help!

### Dependencies
Python v3
Snakemake v3.13

### Output locations:

Snakemake log files will appear in `mutex-de-novo/snakemake/snakelogs/`.
Results will appear in `mutex-de-novo/data/output/`.

## Option 1: Sequential reproduction

If your goal is simply to reproduce our analyses one after another without relying on a scheduler (e.g. slurm, PBS), then you're in the right place. This is the slowest way to get the results, but it should work on just about any machine that has the basic dependencies described above. 

### Navigate to the submitters directory

```
cd mutex-de-novo/snakemake/submitters/
```

### Run the workflow

This should run in a non-blocking fashion in the background. The job should not terminate if you lose your connection.

```
source nosched_repro.sh
```

## Option 2: Parallelized reproduction with slurm

If your goal is to reproduce our analyses on a cluster that leverages the slurm scheduler, then follow this set of instructions.

### Navigate to the submitters directory

```
cd mutex-de-novo/snakemake/submitters/
```

### Run the workflow

```
source submit_slurm_repro.sh
```

## Option 3: Parallelized reproduction with PBS

If you want to submit your workflow to the PBS queue, then follow these instructions and let us know if they work. We don't have PBS installed on our cluster so we haven't been able to test/debug this approach, but we'd love to work with you to get it up and running.

### Navigate to the submitters directory

```
cd mutex-de-novo/snakemake/submitters/
```

### Run the workflow

```
source submit_qsub_repro.sh
```
