#!/usr/bin/env python3
"""Prepare a new disposable connected-client world; never rewrite its source."""
from pathlib import Path
import argparse,shutil,json,hashlib
repo=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source',type=Path,default=repo/'build/treasure-qualification/structures')
parser.add_argument('--directory',type=Path,default=repo/'build/treasure-connected')
args=parser.parse_args()
source=args.source.resolve()
root=args.directory.resolve();dest=root/'saves/TreasureConnected'
assert source.is_dir() and not dest.exists(),'Prepare only a new disposable copy'
removed=[]
coast={f'r.{x}.{z}.mca' for x in [-987,-986] for z in [-3239,-3238]}
def ignore(directory,names):
 omit=[]
 for name in names:
  if name in coast or name=='session.lock':
   omit.append(name);removed.append(str((Path(directory)/name).relative_to(source)))
 return omit
before=hashlib.sha256((source/'level.dat').read_bytes()).hexdigest()
shutil.copytree(source,dest,ignore=ignore)
assert before==hashlib.sha256((source/'level.dat').read_bytes()).hexdigest()
assert not any(p.name in coast for p in dest.rglob('*.mca'))
(root/'fixture-copy.json').write_text(json.dumps({'source':str(source),'destination':str(dest),'sourceLevelDatSha256':before,'omittedOnlyFromCopy':removed,'coastalRegionFilesAbsent':True,'purpose':'Real connected-player cold coastal edge; same saved generation definition/seed, spawn retained, test coast and associated section edits absent.'},indent=2))
print(json.dumps({'copy':str(dest),'omitted':removed}))
