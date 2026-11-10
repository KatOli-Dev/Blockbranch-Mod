import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Original small stepped crystals and inlay, with native host/tool sprites preserved as separate layers. */
public class GenerateBismuth {
 static final int[] COLORS={0x343c48,0x567e85,0x9a75a3,0xc79470,0xd1ba7a,0x84a99b};
 static void p(BufferedImage i,int x,int y,int color){i.setRGB(x,y,0xff000000|color);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 static void step(BufferedImage i,int x,int y,int w){for(int b=0;b<w;b++)for(int a=0;a<w;a++){if(a==0&&b==0)continue;int edge=Math.min(Math.min(a,b),Math.min(w-a-1,w-b-1));p(i,x+a,y+b,COLORS[edge==0?0:1+(a+b)%5]);}}
 public static void main(String[] args)throws Exception{
  var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);step(ore,1,2,5);step(ore,9,1,5);step(ore,6,9,6);save(ore,"block/bismuth_ore_overlay");
  var raw=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);step(raw,1,7,7);step(raw,6,1,7);step(raw,8,8,7);save(raw,"item/raw_bismuth");
  var ingot=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;p(ingot,x,y,y==5||x==1||x==14||y==12?COLORS[0]:COLORS[1+(x/3+y/2)%5]);}save(ingot,"item/bismuth_ingot");
  var inlay=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{4,2},{5,2},{6,2},{7,3},{8,4},{9,5}})p(inlay,c[0],c[1],COLORS[1+(c[0]+c[1])%5]);save(inlay,"item/bismuth_pickaxe_inlay");
 }
}
