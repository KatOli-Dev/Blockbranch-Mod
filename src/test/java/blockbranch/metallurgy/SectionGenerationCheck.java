package blockbranch.metallurgy;

import blockbranch.realistic.UndergroundStructure;
import java.lang.reflect.Method;
import java.util.SplittableRandom;

/**
 * Whole-section fast paths against the per-block originals, for every block of each sampled
 * section: {@link SectionVeins} against each {@code <Ore>Veins.contains}, and the
 * {@link UndergroundStructure.Section} density against {@code density}, compared as raw bits.
 */
public final class SectionGenerationCheck {
    private static void check(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    private interface Vein { boolean contains(long seed,int x,int y,int z,int depth); }
    private static final Vein[] ORACLES={TinVeins::contains,ZincVeins::contains,ManganeseVeins::contains,TungstenVeins::contains,
        NickelVeins::contains,ChromiumVeins::contains,LeadVeins::contains,FoolsDiamondVeins::contains,SilverVeins::contains,
        PlatinumVeins::contains,TitaniumVeins::contains,MagnetiteVeins::contains,ResoniteVeins::contains,BismuthVeins::contains,
        UraniumVeins::contains};

    public static void main(String[] args) throws Exception {
        check(ORACLES.length==SectionVeins.RULES.length,"One oracle per rule");
        var random=new SplittableRandom(20261108);
        long veinBlocks=0,hits=0,caveBlocks=0,voids=0;int sections=0;
        Method density=UndergroundStructure.class.getDeclaredMethod("sectionDensity",int.class,int.class,int.class,int.class,UndergroundStructure.Section.class);
        density.setAccessible(true);
        for(int sample=0;sample<240;sample++){
            long seed=sample<4?new long[]{0,424242,-1,Long.MIN_VALUE}[sample]:random.nextLong();
            // Origins near zero cross every negative/positive cell and lattice boundary;
            // later ones reach far coordinates. Section Y covers the whole build range.
            int range=sample<120?64:1_000_000;
            int cx=random.nextInt(-range,range),cz=random.nextInt(-range,range),sy=random.nextInt(-130,130);
            int x0=cx<<4,z0=cz<<4,y0=sy<<4;
            // Surfaces put each block between 0 and about 260 blocks deep (or above ground),
            // so every ore window and both density slopes are exercised. Uranium (depth>8000)
            // gets a deep column set of its own.
            var ground=new int[256];int base=y0+random.nextInt(-40,280),deepBase=y0+8000+random.nextInt(0,2000);
            boolean deep=sample%8==7;
            for(int i=0;i<256;i++)ground[i]=(deep?deepBase:base)+random.nextInt(-24,25);
            var veins=new SectionVeins(seed,x0,y0,z0,ground);
            for(int ore=0;ore<ORACLES.length;ore++)
                for(int ly=0;ly<16;ly++)for(int lz=0;lz<16;lz++)for(int lx=0;lx<16;lx++){
                    int x=x0+lx,y=y0+ly,z=z0+lz;
                    boolean expected=ORACLES[ore].contains(seed,x,y,z,ground[lz*16+lx]-y);
                    check(veins.contains(ore,lx,ly,lz)==expected,"Vein "+ore+" seed="+seed+" at "+x+","+y+","+z);
                    veinBlocks++;if(expected)hits++;
                }
            var field=new UndergroundStructure(seed);var section=field.section(x0,y0,z0);
            int surface=y0+random.nextInt(-20,80);
            for(int ly=0;ly<16;ly++)for(int lz=0;lz<16;lz++)for(int lx=0;lx<16;lx++){
                int x=x0+lx,y=y0+ly,z=z0+lz,s=surface+((lx*7+lz*3)&15);
                double expected=field.density(x,y,z,s),actual=(double)density.invoke(field,x,y,z,s,section);
                check(Double.doubleToRawLongBits(expected)==Double.doubleToRawLongBits(actual),"Density seed="+seed+" at "+x+","+y+","+z);
                check(field.voidAt(x,y,z,s,section)==field.voidAt(x,y,z,s),"Void decision");
                caveBlocks++;if(expected<0)voids++;
            }
            // Blocks outside the section fall back to the direct path.
            check(field.voidAt(x0-1,y0,z0,surface,section)==field.voidAt(x0-1,y0,z0,surface),"Outside block fallback");
            sections++;
        }
        check(hits>0&&voids>0,"Samples reach both outcomes");
        System.out.println("PASS section generation sections="+sections+" veinBlocks="+veinBlocks+" veinHits="+hits+" caveBlocks="+caveBlocks+" voids="+voids);
    }
}
