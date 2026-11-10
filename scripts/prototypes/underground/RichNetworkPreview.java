package blockbranch.realistic;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;

/** Identical-coordinate voxel cross sections: analytic geometry, not gameplay footage. */
public final class RichNetworkPreview {
    public static void main(String[] args)throws Exception{
        long[] seeds={424242,1,-17};int[][] positions={{237582,-277,-262158},{57336,-214,-57376},{-122868,-285,180198}};
        var image=new BufferedImage(1060,1320,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setColor(new Color(28,30,31));g.fillRect(0,0,1060,1320);g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,21));g.drawString("ANALYTIC RICH REVISION — NOT IN GAME",20,30);
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));
        g.drawString("Frozen field (left) / bounded rich revision (right), identical coordinates",20,58);
        g.drawString("256 × 192m slices through qualified witnesses. Grey rock / dark cave void.",20,82);
        for(int index=0;index<3;index++){
            long seed=seeds[index];int[] p=positions[index];int ox=p[0]-128,oy=p[1]+96,oz=p[2];
            var terrain=new DrainageNetwork(new TerrainShape(seed),seed,true,true);
            var old=new UndergroundStructure(seed);var rich=new RichNetworkRevision(seed,(x,z)->terrain.sample(x,z).height());
            int top=115+index*400;g.setColor(Color.WHITE);g.drawString("Seed "+seed+"; z="+oz,20,top-9);
            for(int x=0;x<256;x++){
                int ground=terrain.sample(ox+x,oz).height();
                for(int y=0;y<192;y++)for(int mode=0;mode<2;mode++){
                    boolean air=mode==0?old.voidAt(ox+x,oy-y,oz,ground):rich.voidAt(ox+x,oy-y,oz,ground);
                    g.setColor(air?new Color(16,19,20):new Color(91,88,82));
                    g.fillRect(16+mode*524+x*2,top+y*2,2,2);
                }
            }
        }
        g.dispose();Path out=Path.of("build/underground-rich-network-revision/analytic-comparison.png");
        ImageIO.write(image,"png",out.toFile());System.out.println(out);
    }
}
