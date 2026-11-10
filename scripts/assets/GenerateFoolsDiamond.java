import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Original 16x tell and dust only; native diamond-ore artwork remains a resource-pack reference. */
public class GenerateFoolsDiamond {
 static void p(BufferedImage i,int x,int y,int c){i.setRGB(x,y,0xff000000|c);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 public static void main(String[] args)throws Exception{
  // The same small dull-gold fracture occupies the lower-left of every face, on both hosts.
  var tell=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);p(tell,2,12,0x958b76);p(tell,3,13,0x6f675b);p(tell,4,14,0x958b76);save(tell,"block/fools_diamond_tell");
  var dust=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=7;y<=13;y++)for(int x=2;x<=13;x++){if((y<10&&(x<6-(y-7)||x>9+(y-7)))||(y==13&&(x==2||x==13)))continue;p(dust,x,y,y==13||x==2?0x4e6970:(x+y)%4==0?0xa4c0c3:0x789799);}p(dust,5,8,0x958b76);p(dust,10,11,0xb5ac92);save(dust,"item/fools_diamond_dust");
 }
}
