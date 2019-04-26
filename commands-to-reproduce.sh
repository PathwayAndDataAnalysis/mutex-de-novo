#!/bin/bash

# Randomization amount to use for matrix shuffling
ITER="10000"

# Prefix of the command to run mutex-de-novo
CMD="java -jar mutex-de-novo.jar"

# FDR cutoffs to use in exploring the result sizes
FDRS="0.01 0.05 0.1 0.2 0.3 0.4 0.5"

# The next 10 runs have high execution time. They can run in parallel on a cluster.

# 1. Generate original results (major results in the paper) on SFARI genes (Table 1)
$CMD calculate mssng-ssc-autism SFARI SFARI-original $ITER

# 2. Generate original results (major results in the paper) on Reactome pathways (Table 4)
$CMD calculate mssng-ssc-autism Reactome Reactome-original $ITER

# 3,4. Use only introns (Table 2)
$CMD calculate mssng-ssc-autism-intron SFARI SFARI-intron $ITER
$CMD calculate mssng-ssc-autism-intron Reactome Reactome-intron $ITER

# 5,6. Use only non-introns (Table 3)
$CMD calculate mssng-ssc-autism-not-intron SFARI SFARI-not-intron $ITER
$CMD calculate mssng-ssc-autism-not-intron Reactome Reactome-not-intron $ITER

# 7,8. MSSNG only
$CMD calculate mssng-autism SFARI SFARI-MSSNG $ITER
$CMD calculate mssng-autism Reactome Reactome-MSSNG $ITER

# 9,10. SSC only
$CMD calculate ssc-autism SFARI SFARI-SSC $ITER
$CMD calculate ssc-autism Reactome Reactome-SSC $ITER

# 11,12. Controls only
$CMD calculate mssng-ssc-control SFARI SFARI-controls $ITER
$CMD calculate mssng-ssc-control Reactome Reactome-controls $ITER

# 13,14. Replace the autism samples from SSC dataset with equal amounts of control samples
$CMD calculate mssng-autism-ssc-control SFARI SFARI-MSSNG-autism-SSC-control $ITER
$CMD calculate mssng-autism-ssc-control Reactome Reactome-MSSNG-autism-SSC-control $ITER

# The rest need to be executed after the above runs finish, and these have low execution time.

# 15. Explore the result size of Reactome pathways under different filtering conditions and FDR cutoff
$CMD explore-significance-in-results Reactome-original/results.txt Reactome-original/results-significance-explored.txt mutex $FDRS

# 16. Do the same for MSSNG-only run
$CMD explore-significance-in-results Reactome-MSSNG/results.txt Reactome-MSSNG/results-significance-explored.txt mutex $FDRS

# 17. Do the same for SSC-only run
$CMD explore-significance-in-results Reactome-SSC/results.txt Reactome-SSC/results-significance-explored.txt mutex $FDRS

# 18. Do the same for controls run
$CMD explore-significance-in-results Reactome-controls/results.txt Reactome-controls/results-significance-explored.txt mutex $FDRS

# 19. Do the same for the run where SSC controls used instead of autism samples
$CMD explore-significance-in-results Reactome-MSSNG-autism-SSC-control/results.txt Reactome-MSSNG-autism-SSC-control/results-significance-explored.txt mutex $FDRS

# Rest of the comments will be decided in the following days.

# 20. Generate table for pathway results for testing highest mutated 50 pathways (Table 5)
#$CMD filter-results-to-most-hit Reactome-original/results-with-names.txt Reactome-original/results-top50.txt mutex 50

# 21. Generate table for pathway results for MSSNG-only run, testing highest mutated 50 pathways (Table 8)
#$CMD filter-results-to-most-hit Reactome-MSSNG/results-with-names.txt Reactome-MSSNG/results-top50.txt mutex 50

# 22. Generate table for pathway results for the run where ASD samples from SSC dataset is replaced with controls, testing highest mutated 50 pathways (Table 9)
#$CMD filter-results-to-most-hit Reactome-MSSNG-autism-SSC-control/results-with-names.txt Reactome-MSSNG-autism-SSC-control/results-top50.txt mutex 50

# 23. Generate a table for Circadian Clock genes (Table 6)
#$CMD annotate-set-members Reactome-original/R-HSA-400253-mutex.txt mssng-ssc-autism Reactome-original/results-circadian-members-table.txt

# 24. Generate a table for PI3K/AKT Signaling genes (Table 7)
#$CMD annotate-set-members Reactome-original/R-HSA-1257604-mutex.txt mssng-ssc-autism Reactome-original/results-PI3K-members-table.txt
