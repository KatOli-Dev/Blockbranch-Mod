import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;
/** Original 16x resonite art and synthesized chime. Opt-in generation needs JDK25 and FFmpeg. */
public class GenerateResonite {
 static void p(BufferedImage i,int x,int y,int c){i.setRGB(x,y,0xff000000|c);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 public static void main(String[] args)throws Exception{
  int dark=0x234758,base=0x4caaa4,light=0xb2e8ca;
  var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{2,1,3,5},{9,4,4,5},{3,11,3,4},{12,12,2,3}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if((y==c[1]||y==c[1]+c[3]-1)&&x==c[0])continue;p(ore,x,y,x==c[0]?dark:x==c[0]+1?light:base);}save(ore,"block/resonite_ore_overlay");
  var crystal=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=1;y<15;y++)for(int x=3;x<=12;x++){int span=y<5?y+1:y>11?15-y:5;if(Math.abs(x-7)>span)continue;p(crystal,x,y,x<=4||y==14?dark:x<7?light:x==7?0x79cdb9:base);}save(crystal,"item/resonite_crystal");
  var block=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<16;y++)for(int x=0;x<16;x++){int d=Math.min(Math.min(x,15-x),Math.min(y,15-y));p(block,x,y,d==0?dark:d==1?base:x+y<13?0x74bdb1:x-y>3?0x347a80:0x4b9697);}for(int i=3;i<=11;i++){p(block,i,3,light);p(block,3,i,0x79cdb9);}save(block,"block/resonite_block");
  var sound=Path.of("src/main/resources/assets/blockbranch/sounds/resonite");Files.createDirectories(sound);var tmp=Path.of("build/resonite-audio");Files.createDirectories(tmp);
  for(int variant=1;variant<=3;variant++){
   int rate=44100,n=(int)(rate*1.2);byte[] pcm=new byte[n*2];double f=740*Math.pow(2,(variant-2)/24.0);
   for(int i=0;i<n;i++){double t=i/(double)rate,attack=Math.min(1,t/.004),tail=Math.min(1,(1.2-t)/.08);double v=0;double[] ratios={1,1.5,2.01,2.76};for(int k=0;k<ratios.length;k++)v+=Math.sin(2*Math.PI*f*ratios[k]*t)*Math.exp(-t*(3.4+k*1.7))/(k+1);short sample=(short)Math.round(10000*v*attack*tail);pcm[i*2]=(byte)sample;pcm[i*2+1]=(byte)(sample>>8);}
   var wav=tmp.resolve("chime"+variant+".wav");try(var stream=new AudioInputStream(new java.io.ByteArrayInputStream(pcm),new AudioFormat(rate,16,1,true,false),n)){AudioSystem.write(stream,AudioFileFormat.Type.WAVE,wav.toFile());}
   if(new ProcessBuilder("ffmpeg","-loglevel","error","-y","-i",wav.toString(),"-c:a","libvorbis","-q:a","5",sound.resolve("chime"+variant+".ogg").toString()).inheritIO().start().waitFor()!=0)throw new IllegalStateException("FFmpeg chime encoding failed");
  }
 }
}
