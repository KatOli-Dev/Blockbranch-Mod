import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Original 16x zinc and brass art; native ore hosts remain resource-pack textures. */
public class GenerateZincBrass {
    private static void p(BufferedImage image,int x,int y,int color){if(x>=0&&x<16&&y>=0&&y<16)image.setRGB(x,y,0xff000000|color);}
    private static void save(BufferedImage image,String name)throws Exception{var path=Path.of("src/main/resources/assets/blockbranch/textures/"+name+".png");Files.createDirectories(path.getParent());ImageIO.write(image,"png",path.toFile());}
    public static void main(String[] args)throws Exception{
        var ore=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int[] piece:new int[][]{{2,2,3,3},{9,3,4,3},{4,9,4,3},{11,11,3,3}})for(int y=piece[1];y<piece[1]+piece[3];y++)for(int x=piece[0];x<piece[0]+piece[2];x++){
            if((x==piece[0]&&y==piece[1])||(x==piece[0]+piece[2]-1&&y==piece[1]+piece[3]-1))continue;
            p(ore,x,y,y==piece[1]?0xd2cbd4:x==piece[0]?0x77717f:0xa39aaa);
        }save(ore,"block/zinc_ore_overlay");
        var block=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)p(block,x,y,x==0||y==0?0xe5cf7a:x==15||y==15?0x8c763a:x+y<10?0xd1b85e:0xc4a852);
        save(block,"block/brass_block");
        var bars=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(x==3||x==4||x==7||x==8||x==11||x==12||y==4||y==5||y==12||y==13)p(bars,x,y,x==3||x==7||x==11||y==4||y==12?0xe5cf7a:0x9b813c);
        save(bars,"block/brass_bars");
        ingot("zinc_ingot",0x68616f,0xa39aaa,0xd2cbd4);ingot("brass_ingot",0x79612e,0xc4a852,0xe5cf7a);
        var raw=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int[] part:new int[][]{{2,7,6,6},{7,3,6,6},{8,10,6,4}})for(int y=part[1];y<part[1]+part[3];y++)for(int x=part[0];x<part[0]+part[2];x++){
            if((x==part[0]||x==part[0]+part[2]-1)&&(y==part[1]||y==part[1]+part[3]-1))continue;
            p(raw,x,y,y==part[1]?0xd2cbd4:x==part[0]||y==part[1]+part[3]-1?0x68616f:(x+y)%5==0?0x786e64:0xa39aaa);
        }save(raw,"item/raw_zinc");
    }
    private static void ingot(String name,int dark,int base,int light)throws Exception{
        var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
        for(int y=5;y<=12;y++)for(int x=1;x<=14;x++){
            if(y<7&&(x<4-(y-5)||x>11+(y-5))||y==12&&(x==1||x==14))continue;
            p(image,x,y,y==5||x==1||x==14||y==12?dark:y<8?light:base);
        }save(image,"item/"+name);
    }
}
