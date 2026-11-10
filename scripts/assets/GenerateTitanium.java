import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Original 16x titanium materials/boots and native boot UV atlases; existing art is untouched. */
public class GenerateTitanium {
 static void p(BufferedImage i,int x,int y,int c){i.setRGB(x,y,0xff000000|c);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 static void ingot(String n,int dark,int base,int light)throws Exception{var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;p(i,x,y,y==5||x==1||x==14||y==12?dark:y<8?light:base);}save(i,"item/"+n);}
 static void rect(BufferedImage i,int x,int y,int w,int h){for(int v=y;v<y+h;v++)for(int u=x;u<x+w;u++)p(i,u,v,v==y?0xedf5f4:u==x||v==y+h-1?0x6c8088:0xb7c6cc);}
 public static void main(String[] args)throws Exception{
  int dark=0x526477,base=0x9bafc2,light=0xdfebf2;var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);int[][] parts={{2,1,3,4},{10,3,4,3},{5,8,3,4},{11,12,3,3}};for(int[] c:parts)for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if(x==c[0]&&y==c[1])continue;p(ore,x,y,y==c[1]?light:x==c[0]||y==c[1]+c[3]-1?dark:base);}save(ore,"block/titanium_ore_overlay");
  var raw=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{1,7,7,6},{7,2,6,7},{8,10,6,4}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if((x==c[0]||x==c[0]+c[2]-1)&&(y==c[1]||y==c[1]+c[3]-1))continue;p(raw,x,y,y==c[1]?light:x==c[0]||y==c[1]+c[3]-1?dark:(x+y)%5==0?0x786e64:base);}save(raw,"item/raw_titanium");ingot("titanium_ingot",dark,base,light);ingot("titanium_alloy_ingot",0x6c8088,0xb7c6cc,0xedf5f4);
  var block=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<16;y++)for(int x=0;x<16;x++)p(block,x,y,x==0||y==0?0xedf5f4:x==15||y==15?0x6c8088:y==4&&x>2&&x<13?0x9bafc2:0xb7c6cc);save(block,"block/titanium_alloy_block");
  var icon=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);String[] shape={" BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB ","BBBBB  BBBBB","BBBBB  BBBBB","BBBBB  BBBBB"};for(int y=0;y<shape.length;y++)for(int x=0;x<shape[y].length();x++)if(shape[y].charAt(x)=='B'){boolean edge=x==0||y==0||x+1==shape[y].length()||y+1==shape.length||shape[y].charAt(x-1)==' '||shape[y].charAt(x+1)==' ';p(icon,x+2,y+4,edge?0x6c8088:y<2?0xedf5f4:0xb7c6cc);}save(icon,"item/titanium_alloy_boots");
  var adult=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);rect(adult,0,27,16,5);save(adult,"entity/equipment/humanoid/titanium_alloy");var baby=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);rect(baby,0,25,12,4);rect(baby,0,29,12,4);save(baby,"entity/equipment/humanoid_baby/titanium_alloy");
 }
}
