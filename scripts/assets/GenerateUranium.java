import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Original pixel overlays/materials; native spear and status-icon dimensions retain their UVs. */
public class GenerateUranium {
 static void p(BufferedImage i,int x,int y,int c){i.setRGB(x,y,0xff000000|c);}
 static void save(BufferedImage i,String n)throws Exception{var f=Path.of("src/main/resources/assets/blockbranch/textures/"+n+".png");Files.createDirectories(f.getParent());ImageIO.write(i,"png",f.toFile());}
 public static void main(String[] args)throws Exception{
  int dark=0x283325,base=0x687944,light=0xb5d360;
  var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{2,2,4,3},{10,5,3,4},{4,10,4,4},{12,12,3,3}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if(x==c[0]&&y==c[1])continue;p(ore,x,y,x==c[0]||y==c[1]+c[3]-1?dark:(x+y)%4==0?light:base);}save(ore,"block/uranium_ore_overlay");
  var raw=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{1,7,7,6},{7,2,6,7},{8,10,6,4}})for(int y=c[1];y<c[1]+c[3];y++)for(int x=c[0];x<c[0]+c[2];x++){if((x==c[0]||x==c[0]+c[2]-1)&&(y==c[1]||y==c[1]+c[3]-1))continue;p(raw,x,y,y==c[1]?light:x==c[0]||y==c[1]+c[3]-1?dark:(x+y)%4==0?0x909e55:base);}save(raw,"item/raw_uranium");
  var ingot=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;p(ingot,x,y,y==5||x==1||x==14||y==12?dark:y<8?light:base);}save(ingot,"item/uranium_ingot");
  var tip=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=2;y<=13;y++)for(int x=2;x<=13;x++){int s=x+y;if(s<11||s>16||x-y<1)continue;p(tip,x,y,s==11||x-y==1?dark:s<14?light:base);}save(tip,"item/uranium_tip");
  for(String type:new String[]{"sword","axe","spear"}){var i=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);int[][] pixels=type.equals("axe")?new int[][]{{9,1},{8,2},{7,3},{6,4}}:new int[][]{{13,1},{14,0},{14,1},{13,2}};for(int[] c:pixels)p(i,c[0],c[1],c[0]%2==0?light:base);save(i,"item/uranium_"+type+"_tip");}
  var held=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);for(int[] c:new int[][]{{0,0},{1,0},{0,1},{1,1},{2,1},{2,2}})p(held,c[0],c[1],(c[0]+c[1])%2==0?light:base);save(held,"item/uranium_spear_in_hand_tip");
  var effect=new BufferedImage(18,18,BufferedImage.TYPE_INT_ARGB);for(int y=1;y<17;y++)for(int x=1;x<17;x++){double dx=x-8.5,dy=y-8.5,r=Math.sqrt(dx*dx+dy*dy),a=Math.atan2(dy,dx);double t=(a+Math.PI*2)%(Math.PI*2/3);if(r<2||r>=4&&r<8&&t<Math.PI/3)p(effect,x,y,r>7?dark:light);}save(effect,"mob_effect/radiation");
 }
}
