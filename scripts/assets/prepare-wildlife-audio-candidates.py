#!/usr/bin/env python3
"""Local audition preparation only. Never edits runtime sounds or downloads audio.
Requires ffmpeg/ffprobe, numpy/scipy/matplotlib. Outputs must stay under build/.
Candidates are signal detections, not species/action identifications or approval.
"""
import argparse,hashlib,json,math,shutil,subprocess,wave
from pathlib import Path
import numpy as np
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from scipy.signal import spectrogram

ROOT=Path(__file__).resolve().parents[2]
def run(args):return subprocess.run(args,check=True,capture_output=True).stdout
def db(v):return float(20*np.log10(max(float(v),1e-10)))
def metrics(samples):
 return {'peak_dbfs':db(np.max(np.abs(samples))), 'rms_dbfs':db(np.sqrt(np.mean(samples.astype(np.float64)**2))),
         'samples_at_or_over_full_scale':int(np.sum(np.abs(samples)>=1.0))}
def main():
 p=argparse.ArgumentParser(description=__doc__)
 p.add_argument('audio',type=Path);p.add_argument('source_manifest',type=Path);p.add_argument('output',type=Path)
 p.add_argument('--threshold-db',type=float,help='Override adaptive RMS gate after inspecting the waveform')
 p.add_argument('--ranges',type=Path,help='Optional visually reviewed JSON [[start_seconds,end_seconds],...] instead of automatic detection')
 p.add_argument('--attack-padding-ms',type=float,default=80,help='Padding before selected ranges; use 0 for already padded explicit ranges')
 p.add_argument('--tail-padding-ms',type=float,default=150,help='Padding after selected ranges')
 p.add_argument('--fade-in-ms',type=float,default=5,help='Fade at each output start; inspect that it ends before the call attack')
 p.add_argument('--fade-out-ms',type=float,default=15,help='Fade at each output end; inspect that it starts after the call tail')
 a=p.parse_args();src=a.audio.resolve();out=a.output.resolve();meta=json.loads(a.source_manifest.read_text())
 for value in [a.attack_padding_ms,a.tail_padding_ms,a.fade_in_ms,a.fade_out_ms]:assert math.isfinite(value) and 0<=value<=5000,'padding/fades must be within 0–5000ms'
 for key in ['title','author','source_url','license','license_url','attribution_text']:assert meta.get(key),f'missing source field {key}'
 assert meta['license'] in ['CC0-1.0','CC-BY-3.0','CC-BY-4.0'],'license must be explicitly cleared before processing'
 assert out.is_relative_to(ROOT/'build') and not out.exists(),'use a new output directory under repository build/'
 assert src.is_file() and src.stat().st_size<=128*1024*1024,'missing/oversized source'
 probe=json.loads(run(['ffprobe','-v','error','-show_streams','-show_format','-of','json',str(src)]))
 stream=next(s for s in probe['streams'] if s['codec_type']=='audio');channels=int(stream['channels'])
 duration=float(probe['format']['duration']);assert 0<duration<=180 and channels in (1,2),'use a bounded mono/stereo source excerpt'
 rate=44100
 decoded=np.frombuffer(run(['ffmpeg','-v','error','-i',str(src),'-map','0:a:0','-ar',str(rate),'-f','f32le','pipe:1']),dtype='<f4').reshape(-1,channels).copy()
 assert len(decoded)>0 and np.isfinite(decoded).all()
 mono=decoded.mean(axis=1);hop=441
 envelope=np.sqrt(np.mean(np.pad(mono,(0,(-len(mono))%hop)).reshape(-1,hop).astype(np.float64)**2,axis=1))
 gate=a.threshold_db if a.threshold_db is not None else min(-18,max(-45,db(np.percentile(envelope,20))+12))
 assert math.isfinite(gate) and -90<=gate<=0
 flags=envelope>=10**(gate/20);edges=np.diff(np.r_[False,flags,False].astype(int));runs=list(zip(np.flatnonzero(edges==1),np.flatnonzero(edges==-1)))
 merged=[]
 for lo,hi in runs:
  if merged and lo-merged[-1][1]<=15:merged[-1][1]=hi
  else:merged.append([int(lo),int(hi)])
 raw_ranges=json.loads(a.ranges.read_text()) if a.ranges else [[lo*.01,hi*.01] for lo,hi in merged if hi-lo>=3]
 assert all(raw_ranges[i][0]<=raw_ranges[i+1][0] for i in range(len(raw_ranges)-1)), 'ranges must be ordered'
 ranges=[]
 for lo,hi in raw_ranges:
  assert math.isfinite(lo) and math.isfinite(hi) and 0<=lo<hi<=len(mono)/rate+.011
  lo=max(0,int((lo-a.attack_padding_ms/1000)*rate));hi=min(len(mono),int((hi+a.tail_padding_ms/1000)*rate))
  if ranges and lo<=ranges[-1][1]:ranges[-1][1]=max(hi,ranges[-1][1])
  else:ranges.append([lo,hi])
 # Preserve complete long calls rather than truncating them to an arbitrary game length.
 peak=max([float(np.max(np.abs(mono[lo:hi]))) for lo,hi in ranges]+[1e-10])
 gain_db=min(6,-3-db(peak));gain=10**(gain_db/20)
 out.mkdir(parents=True);(out/'original').mkdir();original=out/'original'/src.name;shutil.copyfile(src,original)
 clips=[]
 for index,(lo,hi) in enumerate(ranges,1):
  clip=mono[lo:hi].copy()*gain;fi=min(int(a.fade_in_ms/1000*rate),len(clip)//2);fo=min(int(a.fade_out_ms/1000*rate),len(clip)//2)
  if fi:clip[:fi]*=np.linspace(0,1,fi)
  if fo:clip[-fo:]*=np.linspace(1,0,fo)
  name=f'candidate-{index:02d}';wavpath=out/(name+'.wav');oggpath=out/(name+'.ogg')
  with wave.open(str(wavpath),'wb') as w:
   w.setnchannels(1);w.setsampwidth(2);w.setframerate(rate);w.writeframes(np.round(np.clip(clip,-1,1)*32767).astype('<i2').tobytes())
  run(['ffmpeg','-v','error','-i',str(wavpath),'-c:a','libvorbis','-q:a','5',str(oggpath)])
  lossy=np.frombuffer(run(['ffmpeg','-v','error','-i',str(oggpath),'-f','f32le','pipe:1']),dtype='<f4')
  clips.append({'name':name,'source_start_seconds':lo/rate,'source_end_seconds':hi/rate,'duration_seconds':len(clip)/rate,
   'wav':str(wavpath.relative_to(out)),'ogg':str(oggpath.relative_to(out)),'wav_metrics':metrics(clip),'ogg_decode_metrics':metrics(lossy),
   'fade_in_seconds':fi/rate,'fade_out_seconds':fo/rate,
   'review_flags':[flag for condition,flag in [(lo==0,'touches source start; check attack'),(hi==len(mono),'touches source end; check tail'),(len(clip)>6*rate,'long call; visually/audibly review before splitting')] if condition],
   'action_assignment':None,'listening_status':'not auditioned','user_approved':False})
 t=np.arange(len(mono))/rate
 fig,axes=plt.subplots(2,1,figsize=(14,6),sharex=True)
 stride=max(1,len(mono)//100000);axes[0].plot(t[::stride],mono[::stride],lw=.4);axes[0].set_ylabel('Mono amplitude');axes[0].set_ylim(-1,1)
 for i,(lo,hi) in enumerate(ranges,1):axes[0].axvspan(lo/rate,hi/rate,alpha=.18,color='orange');axes[0].text(lo/rate,.9,f'{i}',fontsize=8)
 f,ts,power=spectrogram(mono,fs=rate,nperseg=min(1024,len(mono)),noverlap=min(768,max(0,len(mono)-1)))
 axes[1].pcolormesh(ts,f,10*np.log10(np.maximum(power,1e-12)),shading='auto',vmin=-100,vmax=-20);axes[1].set_ylim(0,10000);axes[1].set_ylabel('Hz');axes[1].set_xlabel('Source seconds')
 fig.suptitle(meta['title']+' — amplitude candidates; species/action and listening UNVERIFIED');fig.tight_layout();fig.savefig(out/'waveform-spectrogram.png',dpi=140);plt.close(fig)
 changes=f'Candidate cuts with{a.attack_padding_ms:g}ms attack/{a.tail_padding_ms:g}ms tail padding,{a.fade_in_ms:g}ms/{a.fade_out_ms:g}ms fades;44100Hz mono channel average; shared gain{gain_db:.2f}dB (at most+6dB); WAV PCM16 and Ogg Vorbis q5. No pitch change, denoising or runtime integration.'
 report={'source':meta,'original_sha256':hashlib.sha256(src.read_bytes()).hexdigest(),'original_copy':str(original.relative_to(out)),
 'probe':probe,'source_decoded_channel_metrics':metrics(decoded),'detection_threshold_dbfs':gate,'selection':'explicit reviewed ranges' if a.ranges else 'automatic amplitude detections; not semantic recognition',
 'modifications':changes,'clips':clips,'status':'candidates require actual waveform inspection and listening; no acceptance inferred'}
 (out/'manifest.json').write_text(json.dumps(report,indent=2)+'\n');(out/'CREDITS.txt').write_text(meta['attribution_text']+'\nLicense: '+meta['license_url']+'\nChanges: '+changes+'\n')
 print(json.dumps({'candidate_count':len(clips),'report':str(out/'manifest.json'),'visualization':str(out/'waveform-spectrogram.png'),'listened':False}))
if __name__=='__main__':main()
