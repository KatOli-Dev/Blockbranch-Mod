package blockbranch.realistic;

import java.util.Arrays;

/** Retained flowers, bounded lit colonies and the full native colour palette. */
public final class FlowerForestPaletteCheck {
    public static void main(String[] args){
        var site=new GroundEcology.Site(.65,.2,.1,0,0,false);
        for(long seed:new long[]{424242,1,-17}){
            int[] before=new int[7],after=new int[7],retainedColours=new int[7],tiles=new int[4096],largeTiles=new int[1024];int total=0,changed=0,fresh=0,added=0;
            for(int z=-256;z<256;z++)for(int x=-256;x<256;x++){
                int old=legacy(seed,x,z,site,true),now=TemperateForestVariants.flowerCover(seed,x,z,site,true);
                require(old>=30?now>=30&&now<=36:now==old||(old==3||old==6)&&now>=30&&now<=36,"Existing flowers retained; additions only replace bare grass or short grass");
                require(TemperateForestVariants.flowerCover(seed,x,z,site,false)==3,"Blocked stems remain clear");
                require(TemperateForestVariants.flowerCover(seed,x,z,site.shade(true),true)==legacy(seed,x,z,site.shade(true),true),"Shaded floor routing remains unchanged");
                if(now>=30){after[now-30]++;fresh++;tiles[((z+256)>>3)*64+((x+256)>>3)]++;largeTiles[((z+256)>>4)*32+((x+256)>>4)]++;require(FlowerMicrosites.accepts(seed^0x464C4F5745525350L,x,z),"New flowers retain rendered-stem microsite spacing");if(old<30)added++;}
                if(old>=30){before[old-30]++;retainedColours[now-30]++;total++;if(old!=now){changed++;require(now==30||now==36,"Only missing end species replace original colours");}}
            }
            require(before[0]==0&&before[6]==0,"Prior mapping is the missing-end-species negative control");
            require(changed>total*.10&&changed<total*.20,"Meaningful accents retain most original colours");
            for(int i=1;i<=5;i++)require(retainedColours[i]>before[i]*.8,"Established native colours retain most original sites");
            for(int count:after)require(count>fresh*.05&&count<fresh*.30,"All seven flowers participate without one dominating");
            require(fresh>total*2.1&&fresh<total*2.4&&added==fresh-total,"Broader colonies retain every original flower");
            long empty=Arrays.stream(tiles).filter(n->n==0).count(),largeEmpty=Arrays.stream(largeTiles).filter(n->n==0).count();
            require(empty>tiles.length*.18&&largeEmpty>largeTiles.length*.04,"Broader colonies retain clear gaps at both scales");
            System.out.printf("FLOWER_FOREST_PALETTE seed=%d priorFlowers=%d flowers=%d added=%d recolouredOriginals=%d empty8=%d empty16=%d prior=%s current=%s%n",seed,total,fresh,added,changed,empty,largeEmpty,Arrays.toString(before),Arrays.toString(after));
        }
        System.out.println("FLOWER_FOREST_SEVEN_SPECIES_RETAINED_SITES_SPACING_PASS");
    }
    /** Prior production selector, including unchanged clearance, shade and microsite decisions. */
    private static int legacy(long seed,int x,int z,GroundEcology.Site site,boolean clear){
        if(!clear)return 3;
        double colony=GroundEcology.colonyStrength(seed,0x464C4F5745525041L,x,z,32,9,20,site);
        double tuft=GroundEcology.colonyStrength(seed,0x464C4F5747524153L,x,z,24,5,12,site);
        long h=OakBirchWoodland.mix(seed^0x464C4F5745524345L^x*0x165667B19E3779F9L^z*0xD1B54A32D192ED03L);double roll=(h>>>11)*0x1.0p-53;
        if(site.shaded())return roll<.3*tuft?4:roll<.18*colony?7:3;
        if(colony>.15&&roll<.8*colony&&FlowerMicrosites.accepts(seed^0x464C4F5745525350L,x,z))return 30+(int)Math.clamp((SurfacePatches.signed(seed^0x464C4F5753504543L,x,z,40)+1)*3.5,0,6);
        return roll<.24*tuft?6:3;
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
