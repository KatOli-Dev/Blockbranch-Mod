#!/usr/bin/env python3
"""Copy a stopped disposable native fixture, retaining explored coast/edits. No cold-region deletion.
Use prepare-treasure-connected.py instead when genuinely unexplored coast is required.
"""
import argparse,contextlib,fcntl,hashlib,json,pathlib,shutil,subprocess
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--source',required=True,type=pathlib.Path)
p.add_argument('--directory',required=True,type=pathlib.Path)
p.add_argument('--immutable-copy',action='store_true',help='Input is an owned stopped copy with session.lock deliberately omitted')
a=p.parse_args();source=a.source.resolve();directory=a.directory.resolve();dest=directory/'saves/TreasureConnected'
assert source.is_dir() and not dest.exists(),'Only a new destination is permitted'
assert not any(f.is_symlink() for f in source.rglob('*')),'Native fixture must not contain symlinks'
assert (source/'session.lock').exists() or a.immutable_copy,'Missing lock requires an explicitly immutable copied input'
with ((source/'session.lock').open('r+b') if (source/'session.lock').exists() else contextlib.nullcontext()) as lock:
 if lock is not None:fcntl.lockf(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
 before=hashlib.sha256((source/'level.dat').read_bytes()).hexdigest()
 shutil.copytree(source,dest,ignore=shutil.ignore_patterns('session.lock'))
 assert before==hashlib.sha256((source/'level.dat').read_bytes()).hexdigest(),'Source changed during copy'
metadata={'sourceLevelDatSha256':before,'retainedExploredCoast':True,'retainedEdits':True,'sourceLockAcquired':(source/'session.lock').exists(),'immutableCopiedInput':a.immutable_copy,'ignored':['session.lock'],'source':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'cpuMax':pathlib.Path('/sys/fs/cgroup/cpu.max').read_text().strip(),'memoryMax':pathlib.Path('/sys/fs/cgroup/memory.max').read_text().strip()}
(directory/'fixture-copy.json').write_text(json.dumps(metadata,indent=2)+'\n')
print(json.dumps(metadata))
