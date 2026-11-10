import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Original 16x manganese resources, steel ingot/block/pickaxe; no published art is read or rewritten. */
public class GenerateManganeseSteel {
    private static void p(BufferedImage image,int x,int y,int color){if(x>=0&&x<16&&y>=0&&y<16)image.setRGB(x,y,0xff000000|color);}
    private static void save(BufferedImage image,String name)throws Exception{var path=Path.of("src/main/resources/assets/blockbranch/textures/"+name+".png");Files.createDirectories(path.getParent());ImageIO.write(image,"png",path.toFile());}
    public static void main(String[] args)throws Exception{
        var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int[] part:new int[][]{{2,3,4,3},{10,2,3,4},{4,10,4,3},{11,11,3,3}})for(int y=part[1];y<part[1]+part[3];y++)for(int x=part[0];x<part[0]+part[2];x++){
            if((x==part[0]&&y==part[1])||(x==part[0]+part[2]-1&&y==part[1]+part[3]-1))continue;
            p(ore,x,y,y==part[1]?0xa29ca8:x==part[0]||y==part[1]+part[3]-1?0x4f4858:0x6c6477);
        }save(ore,"block/manganese_ore_overlay");
        var block=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<16;y++)for(int x=0;x<16;x++)p(block,x,y,x==0||y==0?0xcbd4dc:x==15||y==15?0x515c68:x+y<10?0x9eabb7:0x89949f);save(block,"block/steel_block");
        ingot("manganese_ingot",0x443e4b,0x6c6477,0xa29ca8);ingot("steel_ingot",0x35414c,0x89949f,0xcbd4dc);
        var raw=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int[] part:new int[][]{{2,7,6,6},{7,3,6,6},{8,10,6,4}})for(int y=part[1];y<part[1]+part[3];y++)for(int x=part[0];x<part[0]+part[2];x++){
            if((x==part[0]||x==part[0]+part[2]-1)&&(y==part[1]||y==part[1]+part[3]-1))continue;
            p(raw,x,y,y==part[1]?0xa29ca8:x==part[0]||y==part[1]+part[3]-1?0x443e4b:(x+y)%5==0?0x786e64:0x6c6477);
        }save(raw,"item/raw_manganese");
        var pick=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int i=0;i<10;i++){p(pick,3+i,13-i,0x543a23);p(pick,4+i,13-i,0x987043);}for(int x=3;x<=12;x++){p(pick,x,2,0x35414c);p(pick,x,3,x<6?0x89949f:0xcbd4dc);p(pick,x,4,0x89949f);p(pick,x,5,0x515c68);}for(int y=4;y<=7;y++){p(pick,12,y,0xcbd4dc);p(pick,13,y,0x515c68);}p(pick,12,8,0x35414c);save(pick,"item/steel_pickaxe");
    }
    private static void ingot(String name,int dark,int base,int light)throws Exception{var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;p(image,x,y,y==5||x==1||x==14||y==12?dark:y<8?light:base);}save(image,"item/"+name);}
}
