import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Original 16x alloy art; only these three new sprites are written. */
public class GeneratePlatinumAlloy {
 static void p(BufferedImage i,int x,int y,int c){i.setRGB(x,y,0xff000000|c);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 public static void main(String[] args)throws Exception{
  int dark=0x817b75,base=0xd2cbbc,light=0xf8f0df,seam=0xa6a0b8;
  var ingot=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;p(ingot,x,y,y==5||x==1||x==14||y==12?dark:y<8?light:base);}for(int x=5;x<=10;x++)p(ingot,x,10,seam);save(ingot,"item/platinum_alloy_ingot");
  var block=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<16;y++)for(int x=0;x<16;x++)p(block,x,y,x==0||y==0?light:x==15||y==15?dark:(y==4||y==11)&&x>2&&x<13?seam:base);save(block,"block/platinum_alloy_block");
  var pick=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int k=0;k<10;k++){p(pick,3+k,13-k,0x543a23);p(pick,4+k,13-k,0x987043);}for(int x=3;x<=12;x++){p(pick,x,2,dark);p(pick,x,3,x<6?base:light);p(pick,x,4,x>7?seam:base);p(pick,x,5,dark);}for(int y=4;y<=7;y++){p(pick,12,y,light);p(pick,13,y,dark);}p(pick,12,8,dark);save(pick,"item/platinum_alloy_pickaxe");
 }
}
