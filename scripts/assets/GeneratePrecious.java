import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Original 16x silver/platinum foundations; native host art and approved sprites remain untouched. */
public class GeneratePrecious {
 static void p(BufferedImage i,int x,int y,int c){i.setRGB(x,y,0xff000000|c);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 static void ingot(String n,int dark,int base,int light)throws Exception{var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;p(i,x,y,y==5||x==1||x==14||y==12?dark:y<8?light:base);}save(i,"item/"+n);}
 public static void main(String[] a)throws Exception{
  for(String metal:new String[]{"silver","platinum"}){boolean s=metal.equals("silver");int dark=s?0x707080:0x827963,base=s?0xb5b4c8:0xc6bda6,light=s?0xe7e4ef:0xeee5cc;var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);int[][] parts=s?new int[][]{{1,2,3,3},{9,1,3,4},{5,7,3,3},{11,10,4,3},{2,12,2,3}}:new int[][]{{3,2,3,3},{10,6,3,4},{3,11,4,3}};for(int[] c:parts)for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if(x==c[0]&&y==c[1])continue;p(ore,x,y,y==c[1]?light:x==c[0]||y==c[1]+c[3]-1?dark:base);}save(ore,"block/"+metal+"_ore_overlay");
   var raw=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{1,7,7,6},{7,2,6,7},{8,10,6,4}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if((x==c[0]||x==c[0]+c[2]-1)&&(y==c[1]||y==c[1]+c[3]-1))continue;p(raw,x,y,y==c[1]?light:x==c[0]||y==c[1]+c[3]-1?dark:(x+y)%5==0?0x786e64:base);}save(raw,"item/raw_"+metal);ingot(metal+"_ingot",dark,base,light);
   var b=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<16;y++)for(int x=0;x<16;x++){int col=x==0||y==0?light:x==15||y==15?dark:base;if(s&&y==3&&x>2&&x<13)col=0xd1cfdd;if(!s&&(x==2||y==2||x==13||y==13))col=(x==2||y==2)?0xe2d9c2:0xaaa18a;p(b,x,y,col);}save(b,"block/"+metal+"_block");
  }
  var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int k=0;k<10;k++){p(i,3+k,13-k,0x543a23);p(i,4+k,13-k,0x987043);}for(int x=3;x<=12;x++){p(i,x,2,0x707080);p(i,x,3,x<6?0xb5b4c8:0xe7e4ef);p(i,x,4,0xb5b4c8);p(i,x,5,0x707080);}for(int y=4;y<=7;y++){p(i,12,y,0xe7e4ef);p(i,13,y,0x707080);}p(i,12,8,0x707080);save(i,"item/silver_pickaxe");
 }
}
