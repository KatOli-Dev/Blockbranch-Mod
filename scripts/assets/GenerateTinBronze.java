import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;
import java.util.Random;

/** Optional authoring tool for original 16x pixel assets. No vanilla pixels are copied. */
public class GenerateTinBronze {
    private static final Path ROOT=Path.of("src/main/resources/assets/blockbranch/textures");
    private static BufferedImage image(){return new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);}
    private static void pixel(BufferedImage image,int x,int y,int color){if(x>=0&&x<16&&y>=0&&y<16)image.setRGB(x,y,0xff000000|color);}
    private static void save(BufferedImage image,String path)throws Exception{var file=ROOT.resolve(path+".png");Files.createDirectories(file.getParent());ImageIO.write(image,"png",file.toFile());}
    public static void main(String[] args)throws Exception{
        var ore=image();int[][] pieces={{2,3,4,2},{4,5,2,2},{10,2,3,2},{11,4,2,2},{3,10,3,2},{5,12,3,2},{11,10,3,3}};
        for(var p:pieces)for(int y=p[1];y<p[1]+p[3];y++)for(int x=p[0];x<p[0]+p[2];x++){
            if((x==p[0]&&y==p[1]+p[3]-1)||(x==p[0]+p[2]-1&&y==p[1]))continue;
            pixel(ore,x,y,y==p[1]?0xc8d6d5:y==p[1]+p[3]-1?0x657c82:0x93abae);
        }
        save(ore,"block/tin_ore_overlay");
        var block=image();var random=new Random(731);for(int y=0;y<16;y++)for(int x=0;x<16;x++){
            int diagonal=x+y;
            int light=x==0||y==0?14:x==15||y==15?-19:diagonal<6?7:diagonal<10?26:diagonal<13?14:diagonal<22?0:-9;
            light+=random.nextInt(5)-2;
            pixel(block,x,y,((190+light)<<16)|((128+light)<<8)|(65+light));
        }
        save(block,"block/bronze_block");
        ingot("tin_ingot",new int[]{0x5b7279,0x8ea7ad,0xc9d7d7,0x718d95});
        ingot("bronze_ingot",new int[]{0x694020,0xb6753d,0xe0aa61,0x8a532e});
        var raw=image();for(int[] part:new int[][]{{2,6,6,6},{7,3,6,6},{8,9,5,4}}){
            for(int y=part[1];y<part[1]+part[3];y++)for(int x=part[0];x<part[0]+part[2];x++){
                if((x==part[0]||x==part[0]+part[2]-1)&&(y==part[1]||y==part[1]+part[3]-1))continue;
                pixel(raw,x,y,(x+y)%5==0?0x716951:y<part[1]+2?0xc8d5d2:x==part[0]||y==part[1]+part[3]-1?0x5f777c:0x91a8a8);
            }
        }save(raw,"item/raw_tin");
        var pick=image();for(int i=0;i<10;i++){pixel(pick,3+i,13-i,0x543a23);pixel(pick,4+i,13-i,0x987043);}
        for(int x=3;x<=12;x++){pixel(pick,x,2,0x704421);pixel(pick,x,3,x<6?0xc28a4c:0xe0aa61);pixel(pick,x,4,0xb6753d);pixel(pick,x,5,0x80502d);}
        for(int y=4;y<=7;y++){pixel(pick,12,y,0xd69b52);pixel(pick,13,y,0x8a532e);}pixel(pick,12,8,0x704421);
        save(pick,"item/bronze_pickaxe");
    }
    private static void ingot(String name,int[] colors)throws Exception{
        var icon=image();for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){
            if(y<7&&(x<4-(y-5)||x>11+(y-5)))continue;
            if(y==12&&(x==1||x==14))continue;
            pixel(icon,x,y,y==5||x==1||x==14||y==12?colors[0]:y<8?colors[2]:x<4?colors[3]:colors[1]);
        }save(icon,"item/"+name);
    }
}
