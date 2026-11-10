package blockbranch.realistic;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Saved sanctuary stonework 1. Shell replacements preserve every flooded opening;
 * crown additions stay above both swimming rings and inside the original bounds. */
final class SanctuaryStonework {
    @FunctionalInterface interface Writer { void set(int x,int y,int z,Block block); }
    private SanctuaryStonework() {}

    static void hall(Writer w,int x,int y,int z,int mx,int my,int mz) {
        boolean alongX=mx-x>mz-z;
        int start=alongX?x:z,end=alongX?mx:mz;
        for(int side:new int[]{alongX?z:x,alongX?mz:mx}) {
            for(int a=start;a<=end;a++) {
                wall(w,alongX,a,y-1,side,Blocks.DARK_PRISMARINE);
                wall(w,alongX,a,my,side,Blocks.PRISMARINE);
            }
            for(int a=start+4;a<end-2;a+=8) {
                // Window jambs lie beside, never inside, the existing three-wide aperture.
                for(int post:new int[]{a-1,a+3})for(int h=y;h<my;h++)
                    wall(w,alongX,post,h,side,Blocks.DARK_PRISMARINE);
                for(int h:new int[]{y,my-1})for(int b=a;b<=a+2;b++)
                    wall(w,alongX,b,h,side,Blocks.PRISMARINE);
                wall(w,alongX,a+1,my,side,Blocks.SEA_LANTERN);
            }
        }
    }

    static void tower(Writer w,int x,int z,int height) {
        // Legacy decorative bands crossed the bottom row of the upper north/south gates.
        // Restore that row only for this saved definition; old pieces replay unchanged.
        for(int a=2;a<=6;a++)for(int side:new int[]{0,8})w.set(x+a,12,z+side,Blocks.WATER);
        // The two four-high cross passages and the seven-wide shaft retain their water.
        for(int a=0;a<=8;a++)for(int y=1;y<height;y++) {
            if((y<=4||y>=12&&y<=15)&&a>=2&&a<=6)continue;
            Block block=a==0||a==8?Blocks.PRISMARINE_BRICKS:Blocks.PRISMARINE;
            if(y==5||y==11||y==16||y==height-2)block=Blocks.DARK_PRISMARINE;
            for(int side:new int[]{0,8}) {
                w.set(x+a,y,z+side,block);
                w.set(x+side,y,z+a,block);
            }
        }
        // Recessed panels and lanterns articulate all four upper faces.
        for(int center=20;center<height-2;center+=6)for(int a=2;a<=6;a++)for(int y=center-2;y<=center+2&&y<height-2;y++) {
            Block block=Math.abs(a-4)+Math.abs(y-center)<=2?Blocks.DARK_PRISMARINE:Blocks.PRISMARINE;
            if(a==4&&y==center)block=Blocks.SEA_LANTERN;
            for(int side:new int[]{0,8}) {
                w.set(x+a,y,z+side,block);
                w.set(x+side,y,z+a,block);
            }
        }
        // A shallow stepped crown frames the existing five-wide open light well.
        for(int a=-1;a<=9;a++)for(int b=-1;b<=9;b++) {
            if(a==-1||a==9||b==-1||b==9)w.set(x+a,height-1,z+b,Blocks.PRISMARINE_BRICKS);
            if(a>=0&&a<=8&&b>=0&&b<=8&&(a<2||a>6||b<2||b>6))
                w.set(x+a,height,z+b,Blocks.DARK_PRISMARINE);
            if(a>=1&&a<=7&&b>=1&&b<=7&&(a==1||a==7||b==1||b==7))
                w.set(x+a,height+1,z+b,Blocks.PRISMARINE_BRICKS);
        }
        for(int a:new int[]{1,7})for(int b:new int[]{1,7})w.set(x+a,height+2,z+b,Blocks.SEA_LANTERN);
    }

    private static void wall(Writer w,boolean alongX,int a,int y,int side,Block block) {
        w.set(alongX?a:side,y,alongX?side:a,block);
    }
}
