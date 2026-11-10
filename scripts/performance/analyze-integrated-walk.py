#!/usr/bin/env python3
"""Join exported native JFR events and exact frame counters; never infer GPU time."""
import collections,csv,json,pathlib,sys
p=pathlib.Path(sys.argv[1]);d=json.loads((p/'compact-events.json').read_text());meta=json.loads((p/'metadata.json').read_text());events=d['events'];stacks=d['stacks'];tid=meta['renderThreadId']
frames=[{k:int(v) for k,v in x.items()} for x in csv.DictReader((p/'frames.csv').open())]
ticks=[{k:float(v) if k in ('x','y','z') else int(v) for k,v in x.items()} for x in csv.DictReader((p/'ticks.csv').open())]
resourcePath=p/('scheduler-corrected.csv' if (p/'scheduler-corrected.csv').exists() else 'resources.csv');nativeIds=set(e['osThread'] for e in events if e['thread']==tid and 'osThread' in e);resources=[r for r in csv.DictReader(resourcePath.open()) if r.get('renderTid') and int(r['renderTid']) in nativeIds and r.get('renderRunqueueWaitNs')];origin=ticks[0]['x'];movement=next(x['endEpochMillis'] for x in ticks if x['x']>origin+.05)
def summary(rows):
 a=sorted((x['endNano']-x['startNano'])/1e6 for x in rows)
 if not a:return {'count':0}
 return {'count':len(a),'medianMs':a[len(a)//2],'p95Ms':a[int((len(a)-1)*.95)],'p99Ms':a[int((len(a)-1)*.99)],'maxMs':a[-1],'over100ms':sum(x>=100 for x in a),'over500ms':sum(x>=500 for x in a),'renderCpuTotalMs':sum(x['cpuNs'] for x in rows)/1e6}
def joined(f):
 a,b=f['startEpochMillis'],f['endEpochMillis'];es=[e for e in events if e['ms']<=b and e['ms']+e['durationNs']/1e6>=a];samples=[e for e in es if e['thread']==tid and e['type'] in ('jdk.ExecutionSample','jdk.NativeMethodSample')];intervals=[]
 for e in es:
  if e['type']=='jdk.GCPhasePause':intervals.append((max(a,e['ms']),min(b,e['ms']+e['durationNs']/1e6)))
 merged=[]
 for x,y in sorted(intervals):
  if merged and x<=merged[-1][1]:merged[-1]=(merged[-1][0],max(y,merged[-1][1]))
  else:merged.append((x,y))
 row={**f,'wallMs':(f['endNano']-f['startNano'])/1e6,'renderCpuMs':f['cpuNs']/1e6,'gcOverlapUnionMs':sum(y-x for x,y in merged),'samples':dict(collections.Counter(e['type'] for e in samples)),'nativeLeaves':collections.Counter(stacks[e['stack']][0] for e in samples if e['type']=='jdk.NativeMethodSample' and stacks[e['stack']]).most_common(6),'waits':[dict(e,frames=stacks[e['stack']][:10]) for e in es if e['thread']==tid and e['type'] in ('jdk.JavaMonitorEnter','jdk.JavaMonitorWait','jdk.ThreadPark','jdk.ThreadSleep')]}
 before=[x for x in resources if int(x['epochMillis'])<=a];after=[x for x in resources if int(x['epochMillis'])>=b]
 if before and after:
  x,y=before[-1],after[0];row['schedulerBracket']={'wallMs':int(y['epochMillis'])-int(x['epochMillis']),'renderRunMs':(int(y['renderRunNs'])-int(x['renderRunNs']))/1e6,'renderRunqueueWaitMs':(int(y['renderRunqueueWaitNs'])-int(x['renderRunqueueWaitNs']))/1e6,'periods':int(y['nrPeriods'])-int(x['nrPeriods']),'throttledPeriods':int(y['nrThrottled'])-int(x['nrThrottled'])}
 return row
walking=[f for f in frames if f['phase']==2];moving=[f for f in walking if f['startEpochMillis']>=movement]
worst=sorted(walking,key=lambda f:f['endNano']-f['startNano'],reverse=True)[:10]
later=sorted([f for f in walking if resources and f['startEpochMillis']>=int(resources[0]['epochMillis'])],key=lambda f:f['endNano']-f['startNano'],reverse=True)[:3]
wait=next((e for e in events if e['thread']==tid and e['type']=='jdk.JavaMonitorEnter' and e.get('monitorClass')=='java.util.WeakHashMap' and e['durationNs']>100000000),None)
owner=[e for e in events if e['type']=='blockbranch.SnapshotHold' and wait and e['thread']==wait['previousOwner'] and abs(e['ms']-(wait['ms']+wait['durationNs']/1e6))<20 and e['kind']=='uniform-biomes']
out={'runtime':'4a0628b0346d9643fc882c708fbe169d1d9fb077','metadata':meta,'requestedWalking':summary(walking),'confirmedMovementFrames':summary(moving),'confirmedMovementEpoch':movement,'displacementMetres':ticks[-1]['x']-origin,'walkingSeconds':(max(t['endEpochMillis'] for t in ticks if t['phase']==2)-min(t['startEpochMillis'] for t in ticks if t['phase']==2))/1000,'streamingFirstLast':[ticks[0],ticks[-1]],'worstRequestedWalkingFrames':[joined(f) for f in worst],'laterFramesWithVerifiedScheduler':[joined(f) for f in later],'observedUniformMonitorWait':dict(wait,frames=stacks[wait['stack']][:12]) if wait else None,'matchingOwnerHolds':owner,'verifiedRenderNativeIds':sorted(set(e['osThread'] for e in events if e['thread']==tid and 'osThread' in e)),'observerNote':('Original name-only render fields are invalid; corrected observer used.' if resourcePath.name=='scheduler-corrected.csv' else 'Only rows matching JFR native render identity are used.'),'verifiedSchedulerCoverageStart':resources[0]['epochMillis'] if resources else None,'limitations':['Software rendering and quota pressure; not hardware performance evidence.','Native call samples do not distinguish driver/present waits, native worker waits and GPU execution.','Scheduler counters bracket frames rather than giving exact off-CPU timelines.','Chunk events count distinct observed full-load callbacks, not every generated/provisional column.']}
(p/'diagnosis.json').write_text(json.dumps(out,indent=2)+'\n');print(json.dumps({k:out[k] for k in ['requestedWalking','confirmedMovementFrames','displacementMetres','walkingSeconds']},indent=2))
