package blockbranch.realistic;

import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.nio.file.*;

/** Draws actual analytic carve queries, explicitly not a client capture. */
public final class NetworkCavePreview {
    public static void main(String[] args)throws Exception{
        var field=new NetworkCavePrototype(424242,(x,z)->80);
        var image=new BufferedImage(1024,1120,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        g.setColor(new Color(28,30,31));g.fillRect(0,0,1024,1120);g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,22));g.drawString("ANALYTIC PROTOTYPE — NOT IN GAME",24,32);
        g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));g.drawString("Seed 424242; flat dry surface 80m. Regular loop scaffold needs further work.",24,60);
        g.drawString("Top view: white main routes; orange rough shafts; blue surface access.",24,88);
        for(int nz=0;nz<2;nz++)for(int nx=0;nx<2;nx++){
            g.setColor(new Color(60,60,60));g.drawRect(24+nx*256,108+nz*256,256,256);
            for(var s:field.band(nx,0,nz)){
                g.setColor(s.entrance()?new Color(86,169,225):s.walkableDesign()?Color.LIGHT_GRAY:Color.ORANGE);
                g.setStroke(new BasicStroke(3));g.drawLine(24+(int)s.a().x(),108+(int)s.a().z(),24+(int)s.b().x(),108+(int)s.b().z());
            }
        }
        g.setColor(Color.WHITE);g.drawString("Side section: z=128m; x=0…511m; y=95…−335m. Grey rock / dark carved void.",24,650);
        for(int sy=-21;sy<=5;sy++)for(int sx=0;sx<32;sx++){
            var section=field.section(sx,sy,8);
            for(int ly=0;ly<16;ly++)for(int lx=0;lx<16;lx++){
                int x=sx*16+lx,y=sy*16+ly;if(y>95||y< -335)continue;
                boolean air=section.voidAt(x,y,128,80);
                g.setColor(y>80?new Color(113,148,165):air?new Color(18,20,21):new Color(91,88,82));
                g.fillRect(24+x*2,674+95-y,2,1);
            }
        }
        g.dispose();Path out=Path.of("build/underground-network-prototype/analytic-preview.png");
        ImageIO.write(image,"png",out.toFile());System.out.println(out);
    }
}
