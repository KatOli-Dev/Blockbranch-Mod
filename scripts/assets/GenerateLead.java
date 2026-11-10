import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Original 16x lead and lead/tin building art; existing assets are not read or rewritten. */
public class GenerateLead {
 static void p(BufferedImage i,int x,int y,int c){i.setRGB(x,y,0xff000000|c);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 static void ingot(String n,int dark,int base,int light)throws Exception{var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;p(i,x,y,y==5||x==1||x==14||y==12?dark:y<8?light:base);}save(i,"item/"+n);}
 public static void main(String[] args)throws Exception{
  var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{1,3,4,3},{9,1,3,3},{11,7,4,3},{3,11,3,4},{7,9,2,2}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if(x==c[0]&&y==c[1])continue;p(ore,x,y,y==c[1]?0xb7acc5:x==c[0]||y==c[1]+c[3]-1?0x443d55:0x79738d);}save(ore,"block/lead_ore_overlay");
  var raw=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{1,7,7,6},{7,2,6,7},{8,10,6,4}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if((x==c[0]||x==c[0]+c[2]-1)&&(y==c[1]||y==c[1]+c[3]-1))continue;p(raw,x,y,y==c[1]?0xb7acc5:x==c[0]||y==c[1]+c[3]-1?0x443d55:(x+y)%5==0?0x786e64:0x79738d);}save(raw,"item/raw_lead");
  ingot("lead_ingot",0x443d55,0x79738d,0xb7acc5);ingot("lead_alloy_ingot",0x333349,0x5b5c75,0x9da0b6);
  for(boolean alloy:new boolean[]{false,true}){var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<16;y++)for(int x=0;x<16;x++){int base=alloy?0x5b5c75:0x79738d,light=alloy?0x9da0b6:0xb7acc5,dark=alloy?0x333349:0x443d55;int c=x==0||y==0?light:x==15||y==15?dark:base;if(alloy){if(y==3||y==12)c=dark;if(y==4||y==13)c=0x74778e;if((x==3||x==12)&&(y==2||y==11))c=light;}else if(y==3&&x>2&&x<13)c=0x91899e;p(i,x,y,c);}save(i,"block/"+(alloy?"lead_alloy":"lead")+"_block");}
 }
}
