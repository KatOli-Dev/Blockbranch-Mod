import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Original 16x tungsten resources and heavy tool silhouettes; leaves approved art untouched. */
public class GenerateTungsten {
 static final int DARK=0x292d35,BASE=0x555e6b,LIGHT=0xa0abb6;
 static void p(BufferedImage i,int x,int y,int c){if(x>=0&&x<16&&y>=0&&y<16)i.setRGB(x,y,0xff000000|c);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 public static void main(String[] a)throws Exception{
  var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
  for(int[] c:new int[][]{{2,2,3,4},{10,4,4,3},{5,10,3,4},{12,12,2,2}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if(x==c[0]&&y==c[1])continue;p(ore,x,y,y==c[1]?LIGHT:x==c[0]||y==c[1]+c[3]-1?DARK:BASE);}save(ore,"block/tungsten_ore_overlay");
  var raw=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{1,7,7,6},{7,2,6,7},{9,10,5,4}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if((x==c[0]||x==c[0]+c[2]-1)&&(y==c[1]||y==c[1]+c[3]-1))continue;p(raw,x,y,y==c[1]?LIGHT:x==c[0]||y==c[1]+c[3]-1?DARK:(x+y)%5==0?0x786e64:BASE);}save(raw,"item/raw_tungsten");
  for(String n:new String[]{"tungsten_ingot","tungsten_steel_ingot"}){var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;p(i,x,y,y==5||x==1||x==14||y==12?DARK:y<8?LIGHT:n.startsWith("tungsten_steel")?0x6b7783:BASE);}save(i,"item/"+n);}
  for(String type:new String[]{"pickaxe","axe","shovel","hoe"}){var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int k=0;k<10;k++){p(i,3+k,13-k,0x543a23);p(i,4+k,13-k,0x987043);}
   switch(type){
    case "pickaxe"->{for(int x=2;x<=12;x++){p(i,x,2,DARK);p(i,x,3,LIGHT);p(i,x,4,BASE);p(i,x,5,DARK);}for(int y=4;y<=8;y++){p(i,12,y,LIGHT);p(i,13,y,DARK);}}
    case "axe"->{for(int y=2;y<=8;y++)for(int x=8;x<=14;x++){if((y==2&&x>12)||(y==8&&x<11)||(x==8&&y>5))continue;p(i,x,y,x==14||y==8?DARK:y<4||x==13?LIGHT:BASE);}}
    case "shovel"->{for(int y=1;y<=7;y++)for(int x=8;x<=14;x++){int d=Math.abs(x-11)+Math.abs(y-4);if(d>4)continue;p(i,x,y,d==4?DARK:x+y<15?LIGHT:BASE);}}
    case "hoe"->{for(int x=3;x<=12;x++){p(i,x,3,DARK);p(i,x,4,LIGHT);p(i,x,5,BASE);}for(int y=4;y<=8;y++){p(i,3,y,LIGHT);p(i,4,y,DARK);}}
   }save(i,"item/tungsten_steel_"+type);
  }
 }
}
