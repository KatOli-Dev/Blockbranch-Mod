import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;

/** Original 16x steel tool art; leaves the previously published sprites untouched. */
public class GenerateSteelTools {
    private static final int DARK=0x35414c,BRONZE=0x89949f,LIGHT=0xcbd4dc;
    private static void p(BufferedImage image,int x,int y,int color){if(x>=0&&x<16&&y>=0&&y<16)image.setRGB(x,y,0xff000000|color);}
    public static void main(String[] args)throws Exception{
        for(String type:new String[]{"sword","axe","shovel","hoe"}){
            var image=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);
            for(int i=0;i<10;i++){p(image,3+i,13-i,0x543a23);p(image,4+i,13-i,0x987043);}
            switch(type){
                case "sword"->{
                    // Short grip, crossguard and a tapered diagonal steel blade.
                    for(int y=0;y<10;y++)for(int x=7;x<16;x++)image.setRGB(x,y,0);
                    for(int i=0;i<8;i++){int x=6+i,y=9-i;p(image,x,y,DARK);p(image,x+1,y,LIGHT);p(image,x+1,y+1,BRONZE);}
                    for(int i=0;i<5;i++){p(image,3+i,8+i,DARK);p(image,4+i,8+i,LIGHT);}
                    p(image,2,14,DARK);p(image,3,14,BRONZE);
                }
                case "axe"->{for(int y=2;y<=8;y++)for(int x=8;x<=14;x++){
                    if((y==2&&x>12)||(y==8&&x<11)||(x==8&&y>5))continue;
                    p(image,x,y,x==14||y==8?DARK:y<4||x==13?LIGHT:BRONZE);
                }}
                case "shovel"->{for(int y=1;y<=7;y++)for(int x=8;x<=14;x++){
                    int d=Math.abs(x-11)+Math.abs(y-4);if(d>4)continue;
                    p(image,x,y,d==4?DARK:x+y<15?LIGHT:BRONZE);
                }}
                case "hoe"->{for(int x=3;x<=12;x++){p(image,x,3,DARK);p(image,x,4,LIGHT);p(image,x,5,BRONZE);}
                    for(int y=4;y<=8;y++){p(image,3,y,LIGHT);p(image,4,y,DARK);}
                }
            }
            var file=Path.of("src/main/resources/assets/blockbranch/textures/item/steel_"+type+".png");
            ImageIO.write(image,"png",file.toFile());
        }
    }
}
