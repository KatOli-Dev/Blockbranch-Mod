# Rejected cave-network prototypes

These experiments are outside all production/check source sets and release JARs.
They do not replace the playable natural field or change saved geometry. Both
revisions failed adoption: the scaffold was too regular/sparse, while the rich
revision split chambers, lost cave volume in samples and increased query cost.
Their graph proxies do not establish native traversal or global cave-access rates.
Further architecture expansion is shelved.

After building the main classes, run from the repository root:

```sh
python3 scripts/prototypes/underground/run-experiment.py
python3 scripts/prototypes/underground/run-rich-experiment.py
```

Runners use `JAVA_HOME` when supplied, compile only experiment sources and write
to ignored `build/underground-network-prototype/` and
`build/underground-rich-network-revision/`. Each uses serial baseline/candidate
forks, common coordinates, warmups and fresh descriptor caches. These synthetic
query/allocation comparisons exclude native loading, meshing, network/disk I/O
and FPS; faster simplified geometry is not an equivalent-work win.

Historical methods and measurements remain in Git at
`5e8583861dea9b33ca24ca872630ecab9441340a`, under this path and
`docs/reviews/UNDERGROUND_RICH_NETWORK_REVISION_20261101.md`.
