import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Original pixel armor on the native humanoid UVs; no existing textures are read or rewritten. */
public class GenerateTitaniumArmor {
    private static int shade(int x,int y){return 0xb7c6cc;}
    private static void rect(BufferedImage image,int x,int y,int w,int h){for(int v=y;v<y+h;v++)for(int u=x;u<x+w;u++)image.setRGB(u,v,0xff000000|shade(u,v));}
    private static void clear(BufferedImage image,int x,int y,int w,int h){for(int v=y;v<y+h;v++)for(int u=x;u<x+w;u++)image.setRGB(u,v,0);}
    private static void plate(BufferedImage image,int x,int y,int w,int h){
        for(int v=y;v<y+h;v++)for(int u=x;u<x+w;u++)image.setRGB(u,v,0xff000000|(v==y||u==x?0xedf5f4:v==y+h-1||u==x+w-1?0x9bafc2:0xb7c6cc));
    }
    private static void save(BufferedImage image,String name)throws Exception{var path=Path.of("src/main/resources/assets/blockbranch/textures/"+name+".png");Files.createDirectories(path.getParent());ImageIO.write(image,"png",path.toFile());}
    public static void main(String[] args)throws Exception{
        String[][] shapes={
            {"  BBBBBBBBBB  "," BBBBBBBBBBBB ","BBB        BBB","BBB        BBB","BBB        BBB","BBB        BBB","BBB        BBB","BBB        BBB"},
            {"BBB      BBB","BBBB    BBBB"," BBBBBBBBBB "," BBBBBBBBBB ","  BBBBBBBB  ","  BBBBBBBB  ","  BBBBBBBB  ","  BBBBBBBB  ","  BBBBBBBB  ","  BBBBBBBB  "},
            {" BBBBBBBBBB "," BBBBBBBBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "},
            {" BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB "," BBBB  BBBB ","BBBBB  BBBBB","BBBBB  BBBBB","BBBBB  BBBBB"}
        };
        String[] names={"helmet","chestplate","leggings","boots"};
        for(int i=0;i<3;i++){
            var icon=new BufferedImage(16,16,BufferedImage.TYPE_INT_ARGB);var shape=shapes[i];int ox=(16-shape[0].length())/2,oy=(16-shape.length)/2;
            for(int y=0;y<shape.length;y++)for(int x=0;x<shape[y].length();x++)if(shape[y].charAt(x)=='B'){
                boolean edge=x==0||y==0||x+1==shape[y].length()||y+1==shape.length||shape[y].charAt(x-1)==' '||shape[y].charAt(x+1)==' ';
                icon.setRGB(ox+x,oy+y,0xff000000|(edge?0x6c8088:y<2?0xedf5f4:shade(x,y)));
            }
            save(icon,"item/titanium_alloy_"+names[i]);
        }
        var outer=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);
        rect(outer,0,0,32,16);plate(outer,8,8,8,8);clear(outer,9,10,6,6);clear(outer,8,14,8,2);
        rect(outer,16,16,24,16);plate(outer,20,20,8,12);plate(outer,32,20,8,12);clear(outer,22,20,4,2);
        rect(outer,40,16,16,10);plate(outer,44,20,4,6);plate(outer,52,20,4,6);
        rect(outer,0,27,16,5);plate(outer,4,27,4,5);plate(outer,12,27,4,5);
        save(outer,"entity/equipment/humanoid/titanium_alloy_gear");
        var inner=new BufferedImage(64,32,BufferedImage.TYPE_INT_ARGB);rect(inner,0,16,16,16);plate(inner,4,20,4,12);plate(inner,12,20,4,12);rect(inner,16,29,24,3);
        save(inner,"entity/equipment/humanoid_leggings/titanium_alloy_gear");
        // Native 26.3 baby armor has a separate 64x64 atlas and separate limb islands.
        var baby=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        rect(baby,0,0,34,16);plate(baby,8,8,9,8);clear(baby,9,10,7,6);clear(baby,8,14,9,2);
        rect(baby,0,17,18,8);plate(baby,3,20,6,5);rect(baby,0,36,18,5);
        rect(baby,30,17,10,5);plate(baby,33,20,2,2);rect(baby,30,25,10,5);plate(baby,33,28,2,2);
        rect(baby,18,17,12,7);plate(baby,21,20,3,4);rect(baby,18,24,12,7);plate(baby,21,27,3,4);
        rect(baby,0,25,12,4);rect(baby,0,29,12,4);
        save(baby,"entity/equipment/humanoid_baby/titanium_alloy_gear");
    }
}
