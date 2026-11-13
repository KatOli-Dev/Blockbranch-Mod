package blockbranch.realistic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * Resource-only reed morphology: the saved state names stay {@code tip=false/true}, every variant resolves, the star model
 * keeps its render cost bounded (three planes, six quads), and the 16 x 16 art keeps its culms continuous across stacked
 * blocks while carrying clearly more leaf than the published art. Pixel counts are diagnostics, not an ecology claim.
 */
public final class LakeReedAssetCheck {
    private static final String ROOT="/assets/blockbranch/";
    /** Opaque cells of the published (schema 270-era) textures this art replaced; see git history of scripts/assets/lake_reeds.py. */
    private static final int PUBLISHED_STEM=48,PUBLISHED_TIP=45;

    public static void main(String[] args)throws Exception {
        states();models();textures();
        System.out.println("LAKE_REED_ASSETS_PASS");
    }
    private static JsonObject json(String path)throws Exception {
        try(InputStream in=Objects.requireNonNull(LakeReedAssetCheck.class.getResourceAsStream(ROOT+path),path)){return JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();}
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void states()throws Exception {
        var variants=json("blockstates/lake_reed.json").getAsJsonObject("variants");
        require(variants.keySet().equals(Set.of("tip=false","tip=true")),"Saved block-state names unchanged: "+variants.keySet());
        for(var entry:variants.entrySet()){
            var list=entry.getValue().getAsJsonArray();
            require(list.size()==2,"Two orientations per state: "+entry.getKey());
            var turns=new TreeSet<Integer>();
            for(var element:list){
                var variant=element.getAsJsonObject();
                String model=variant.get("model").getAsString();
                require(model.equals("blockbranch:block/lake_reed_"+(entry.getKey().endsWith("true")?"tip":"stem")),"Model matches state: "+model);
                json("models/block/"+model.substring(model.indexOf('/')+1)+".json");
                turns.add(variant.has("y")?variant.get("y").getAsInt():0);
            }
            require(turns.equals(Set.of(0,90)),"Orientations are a quarter turn apart: "+turns);
        }
    }
    private static void models()throws Exception {
        for(var name:new String[]{"stem","tip"}){
            var model=json("models/block/lake_reed_"+name+".json");
            require(model.get("parent").getAsString().equals("blockbranch:block/lake_reed_star"),"Shared star parent: "+name);
            require(model.getAsJsonObject("textures").get("cross").getAsString().equals("blockbranch:block/lake_reed_"+name),"Own texture: "+name);
        }
        var star=json("models/block/lake_reed_star.json");
        var elements=star.getAsJsonArray("elements");
        require(elements.size()==3,"Three crossed planes: "+elements.size());
        var angles=new TreeSet<Integer>();int quads=0;
        for(var element:elements){
            var plane=element.getAsJsonObject();var from=plane.getAsJsonArray("from");var to=plane.getAsJsonArray("to");
            require(from.get(2).getAsDouble()==to.get(2).getAsDouble(),"Zero-thickness plane");
            require(from.get(0).getAsDouble()>=-4&&to.get(0).getAsDouble()<=20,"Planes stay within four pixels of the block (offset-safe)");
            quads+=plane.getAsJsonObject("faces").size();
            angles.add(plane.has("rotation")?plane.getAsJsonObject("rotation").get("angle").getAsInt():0);
        }
        require(angles.equals(Set.of(-60,0,60)),"Planes spaced 60 degrees apart: "+angles);
        require(quads==6,"Render cost bounded at six quads per reed block (published cross: four): "+quads);
    }
    private static BufferedImage image(String path)throws Exception {
        try(InputStream in=Objects.requireNonNull(LakeReedAssetCheck.class.getResourceAsStream(ROOT+"textures/"+path),path)){
            var image=ImageIO.read(in);require(image.getWidth()==16&&image.getHeight()==16,"16x16 texture only: "+path);return image;
        }
    }
    private static boolean opaque(BufferedImage image,int x,int y){return (image.getRGB(x,y)>>>24)!=0;}
    private static int cells(BufferedImage image){int n=0;for(int y=0;y<16;y++)for(int x=0;x<16;x++)if(opaque(image,x,y))n++;return n;}
    private static void textures()throws Exception {
        var stem=image("block/lake_reed_stem.png");var tip=image("block/lake_reed_tip.png");var item=image("item/lake_reed.png");
        for(int y=0;y<16;y++)for(int x:new int[]{5,10})require(opaque(stem,x,y),"Continuous culm in the stem segment at "+x+","+y);
        for(int x=0;x<16;x++)require(x==5||x==10||!(opaque(stem,x,0)&&opaque(stem,x,15)),"Blades of stacked stem segments never meet across the joint at column "+x);
        for(int x:new int[]{5,10})require(opaque(tip,x,15),"The tip meets the stem on both culms at "+x);
        for(int y=0;y<16;y++)for(int x=0;x<16;x++)require(opaque(item,x,y)==opaque(tip,x,y),"Item icon equals the tip art at "+x+","+y);
        int s=cells(stem),t=cells(tip);
        require(s>=PUBLISHED_STEM*3/2&&s<=PUBLISHED_STEM*3,"Stem leaf mass 1.5x..3x the published art: "+s);
        require(t>=PUBLISHED_TIP*3/2&&t<=PUBLISHED_TIP*3,"Tip leaf mass 1.5x..3x the published art: "+t);
        // Blades must reach well beyond the culms on both sides, not hug them like the published twigs.
        int left=0,right=0;for(int y=0;y<16;y++){if(opaque(stem,0,y)||opaque(stem,1,y))left++;if(opaque(stem,14,y)||opaque(stem,15,y))right++;}
        require(left>=3&&right>=3,"Blades reach both outer borders: "+left+" "+right);
        System.out.println("LAKE_REED_ART stem "+s+" tip "+t+" opaque cells (published "+PUBLISHED_STEM+" / "+PUBLISHED_TIP+")");
    }
}
