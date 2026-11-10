#!/usr/bin/env python3
"""One native fresh-world diagnostic. Keep recording and sampled scheduling evidence local."""
import csv, json, pathlib, re, subprocess, sys, time
root=pathlib.Path(__file__).resolve().parents[2]
directory=root/'build/post-occupancy-walk'
directory.mkdir(parents=True,exist_ok=True)
assert not (directory/'saves/IntegratedWalking').exists(), 'Use a fresh fixture directory'
assert (directory/'walking.jfc').exists(), 'Provide the retained diagnostic JFR configuration'
fields=['epochMillis','monoNs','nrPeriods','nrThrottled','throttledUsec','usageUsec','memoryBytes']
for name in ['render','server']:
 fields += [name+x for x in ['Tid','State','RunNs','RunqueueWaitNs','Switches','Wchan']]
metadata={'source':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'dirtyFiles':subprocess.check_output(['git','status','--porcelain'],cwd=root,text=True).splitlines(),'cpuMax':pathlib.Path('/sys/fs/cgroup/cpu.max').read_text().strip(),'memoryMax':pathlib.Path('/sys/fs/cgroup/memory.max').read_text().strip(),'samplingSeconds':.1,'softwareRendering':True,'note':'Scheduler samples bracket frames; they do not by themselves identify GPU or driver waits.'}
(directory/'capture-provenance.json').write_text(json.dumps(metadata,indent=2)+'\n')
tids={};pid=None;lastIdentification=0
with (directory/'driver.log').open('w') as log,(directory/'resources.csv').open('w') as output:
 writer=csv.DictWriter(output,fieldnames=fields);writer.writeheader()
 proc=subprocess.Popen(['scripts/performance/run-headless-client.sh','./gradlew','--offline','runIntegratedWalking','-PintegratedWalkingDirectory=build/post-occupancy-walk','-x','downloadAssets'],cwd=root,stdout=log,stderr=subprocess.STDOUT)
 try:
  while proc.poll() is None:
   row={'epochMillis':time.time_ns()//1000000,'monoNs':time.monotonic_ns()}
   try:
    stat=dict(x.split() for x in pathlib.Path('/sys/fs/cgroup/cpu.stat').read_text().splitlines())
    for dest,src in [('nrPeriods','nr_periods'),('nrThrottled','nr_throttled'),('throttledUsec','throttled_usec'),('usageUsec','usage_usec')]:row[dest]=stat.get(src,'')
    row['memoryBytes']=pathlib.Path('/sys/fs/cgroup/memory.current').read_text().strip()
    if pid is None and (directory/'process-id.txt').exists():pid=int((directory/'process-id.txt').read_text())
    if pid is not None:
     tasks=pathlib.Path('/proc')/str(pid)/'task'
     if len(tids)<2 and time.monotonic()-lastIdentification>=1:
      lastIdentification=time.monotonic()
      # Native renderer workers may inherit comm="Render thread". A name-only
      # match is not an identity oracle: use the JVM's Java-to-native mapping.
      if any(t.joinpath('comm').read_text().strip()=='Server thread' for t in tasks.iterdir()):
       report=subprocess.check_output(['jcmd',str(pid),'Thread.print'],text=True)
       for name,key in [('Render thread','render'),('Server thread','server')]:
        line=next((x for x in report.splitlines() if x.startswith('"'+name+'"')),None)
        if line:
         value=re.search(r'nid=(0x[0-9a-f]+|[0-9]+)',line).group(1)
         tids[key]=str(int(value,16 if value.startswith('0x') else 10))
       (directory/'native-thread-identities.json').write_text(json.dumps(tids)+'\n')
     for name,tid in tids.items():
      task=tasks/tid
      row[name+'Tid']=tid
      row[name+'State']=task.joinpath('stat').read_text().rsplit(')',1)[1].split()[0]
      sched=task.joinpath('schedstat').read_text().split()
      for suffix,value in zip(['RunNs','RunqueueWaitNs','Switches'],sched):row[name+suffix]=value
      row[name+'Wchan']=task.joinpath('wchan').read_text().strip()
   except (OSError,ValueError,subprocess.SubprocessError,AttributeError):pass
   writer.writerow(row);output.flush();time.sleep(.1)
 finally:
  if proc.poll() is None:proc.terminate()
 sys.exit(proc.wait())
