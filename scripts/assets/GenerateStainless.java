import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Original 16x nickel/chromium and polished alloy art; existing sprites are never read or rewritten. */
public class GenerateStainless {
 static void p(BufferedImage i,int x,int y,int c){if(x>=0&&x<16&&y>=0&&y<16)i.setRGB(x,y,0xff000000|c);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 static void ingot(String n,int dark,int base,int light)throws Exception{var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;p(i,x,y,y==5||x==1||x==14||y==12?dark:y<8?light:base);}save(i,"item/"+n);}
 public static void main(String[] args)throws Exception{
  for(String metal:new String[]{"nickel","chromium"}){boolean n=metal.equals("nickel");int dark=n?0x62654e:0x4a6470,base=n?0xa7aa81:0x96bac6,light=n?0xd9d9ae:0xe0f0f2;int[][] patches=n?new int[][]{{2,3,3,3},{10,1,4,3},{6,8,3,4},{1,12,3,2},{12,11,3,3}}:new int[][]{{1,2,4,2},{9,4,3,4},{3,10,4,3},{12,12,3,2}};var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:patches)for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if(x==c[0]&&y==c[1])continue;p(ore,x,y,y==c[1]?light:x==c[0]||y==c[1]+c[3]-1?dark:base);}save(ore,"block/"+metal+"_ore_overlay");
   var raw=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{2,8,6,6},{6,2,7,7},{9,10,5,4}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if((x==c[0]||x==c[0]+c[2]-1)&&(y==c[1]||y==c[1]+c[3]-1))continue;p(raw,x,y,y==c[1]?light:x==c[0]||y==c[1]+c[3]-1?dark:(x+y)%5==0?0x786e64:base);}save(raw,"item/raw_"+metal);ingot(metal+"_ingot",dark,base,light);
  }
  ingot("stainless_steel_ingot",0x697b88,0xb5c5cd,0xedf4f6);
  var b=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<16;y++)for(int x=0;x<16;x++)p(b,x,y,x==0||y==0?0xedf4f6:x==15||y==15?0x697b88:y==2?0xd4e0e5:x+y<13?0xc5d3da:0xb5c5cd);save(b,"block/stainless_steel_block");
  var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int k=0;k<10;k++){p(i,3+k,13-k,0x543a23);p(i,4+k,13-k,0x987043);}for(int x=3;x<=12;x++){p(i,x,2,0x697b88);p(i,x,3,x<6?0xb5c5cd:0xedf4f6);p(i,x,4,0xb5c5cd);p(i,x,5,0x697b88);}for(int y=4;y<=7;y++){p(i,12,y,0xedf4f6);p(i,13,y,0x697b88);}p(i,12,8,0x697b88);save(i,"item/stainless_steel_pickaxe");
 }
}
