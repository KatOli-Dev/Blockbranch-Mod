package blockbranch.realistic;

import java.util.ArrayList;

/** Finite pure-Java falsification of the rock-bed datum for polar underground rules.
 * New continental ice is overburden, not uplifted rock: eligibility and placement that
 * are defined relative to rock (deep-dark/Ancient City, underground structures, cave
 * entry routes) read the ice-free bed under a sheet, cave approaches plan nothing on or
 * under a sheet, and everything reads the unchanged published surface elsewhere, including
 * legacy alpine caps. Dripstone and sulfur habitat windows use the same rock datum; the production label and
 * placement paths are covered by the native PolarHabitatChecks, and only their depth arithmetic here.
 * Window sites are the fixed native-cost coordinates; counts below are measurements at
 * seed 424242, not Earth-area claims or tuned acceptance thresholds. */
public final class PolarUndergroundCheck {
    static final long SEED=424242;
    static final String[] NAMES={"ordinary-woodland","polar-lowland","polar-upland","polar-coast-reference","ice-rock-edge","ice-freshwater-lid"};
    static final int[][] SITES={{272000,-720000},{-1552000,-1904000},{-1456000,-1904000},{-1616000,-1904000},{-1615115,-1904000},{-1589245,-1966053}};
    /** The saved candidate world's actual Ancient City start chunk (build/polar-ice-evidence/cost-final). */
    static final int CITY_CX=-96960,CITY_CZ=-119002;
    static final int RADIUS=12;

    public static void main(String[] args){
        var polar=new DrainageNetwork(new TerrainShape(SEED),SEED,true,true,true,true,true,true,true);
        var published=new DrainageNetwork(new TerrainShape(SEED),SEED,true,true,true,true,true,true,false);
        var bed=polar.iceReference();
        boolean report=args.length>0&&args[0].equals("report");
        int columnsUnderSheet=0,maskChunksDiffer=0,maskColumnsAlt=0,maskColumnsBed=0,cityGateAlt=0,cityGateBed=0,cityGateDiffer=0,sheetChunks=0;
        for(int s=0;s<SITES.length;s++){
            int ccx=SITES[s][0]>>4,ccz=SITES[s][1]>>4,sheetCols=0,differ=0,alt=0,rock=0,gateAlt=0,gateRock=0;
            for(int cz=ccz-RADIUS;cz<=ccz+RADIUS;cz++)for(int cx=ccx-RADIUS;cx<=ccx+RADIUS;cx++){
                boolean sheet=false;
                for(int z=0;z<4;z++)for(int x=0;x<4;x++)if(polar.continentalIceThickness(cx*16+x*4,cz*16+z*4)>0){sheet=true;sheetCols++;}
                if(sheet)sheetChunks++;
                int a=DeepDarkHabitat.mask(SEED,cx,cz,polar),b=DeepDarkHabitat.mask(SEED,cx,cz,bed);
                if(a!=b)differ++;alt+=Integer.bitCount(a);rock+=Integer.bitCount(b);
                int mx=cx*16+8,mz=cz*16+8;
                boolean ga=DeepDarkHabitat.contains(SEED,mx,DeepDarkHabitat.proposalY(polar.bedHeight(mx,mz)),mz,polar);
                boolean gb=DeepDarkHabitat.contains(SEED,mx,DeepDarkHabitat.proposalY(bed.sample(mx,mz).height()),mz,bed);
                if(ga)gateAlt++;if(gb)gateRock++;
                if(ga!=gb)cityGateDiffer++;
            }
            columnsUnderSheet+=sheetCols;maskChunksDiffer+=differ;maskColumnsAlt+=alt;maskColumnsBed+=rock;cityGateAlt+=gateAlt;cityGateBed+=gateRock;
            if(report)System.out.println("site "+NAMES[s]+": sheet mask-columns="+sheetCols+" chunks with deep-dark mask != ice-free-bed mask="+differ+" eligible columns "+alt+" (bed "+rock+") city first-gate chunks "+gateAlt+" (bed "+gateRock+")");
        }
        if(report){
            int mx=CITY_CX*16+8,mz=CITY_CZ*16+8;
            System.out.println("saved Ancient City start chunk "+CITY_CX+","+CITY_CZ+": ice-inclusive height "+polar.sample(mx,mz).height()+" thickness "+polar.continentalIceThickness(mx,mz)+" bed height "+bed.sample(mx,mz).height()
                +" first-gate(ice-inclusive datum, historical)="+DeepDarkHabitat.eligibleColumn(SEED,mx,mz,polar)+" first-gate(ice-free bed)="+DeepDarkHabitat.eligibleColumn(SEED,mx,mz,bed));
            return;
        }
        // Rock-bed datum agrees with the original foundation under a sheet and the published surface elsewhere.
        int checked=0,sheetPoints=0;
        for(int[] site:SITES)for(int dz=-512;dz<=512;dz+=64)for(int dx=-512;dx<=512;dx+=64){
            int x=site[0]+dx,z=site[1]+dz;checked++;var original=bed.sample(x,z);
            if(polar.continentalIceThickness(x,z)>0){
                sheetPoints++;
                require(polar.bedHeight(x,z)==original.height(),"Bed height equals the original foundation under the sheet at "+x+","+z);
                require(polar.bedSample(x,z).equals(original),"Bed sample is the complete original sample (including buried water) at "+x+","+z);
            }else{
                require(polar.bedSample(x,z)==polar.sample(x,z)&&polar.bedHeight(x,z)==polar.sample(x,z).height(),"Outside a sheet the datum is the published surface at "+x+","+z);
            }
            require(published.bedHeight(x,z)==published.sample(x,z).height()&&published.bedSample(x,z)==published.sample(x,z),"Published/gate-off datum is the unchanged surface at "+x+","+z);
        }
        require(sheetPoints>20,"Nonvacuous sheet points: "+sheetPoints);
        // Alpine land ice keeps the published datum (legacy worlds carried it in the surface).
        require(polar.continentalIceThickness(1039616,1296768)==0&&polar.landIceThickness(1039616,1296768)>0&&polar.bedHeight(1039616,1296768)==polar.sample(1039616,1296768).height(),"Alpine cap datum unchanged");
        // Deep-dark mask and Ancient City first gate follow the ice-free bed exactly.
        require(columnsUnderSheet>0&&sheetChunks>0,"Nonvacuous sheet windows");
        require(maskChunksDiffer==0&&maskColumnsAlt==maskColumnsBed&&cityGateDiffer==0&&cityGateAlt==cityGateBed,
            "Deep-dark masks/city gates read the bed: differing chunks "+maskChunksDiffer+" eligible "+maskColumnsAlt+"/"+maskColumnsBed+" gates "+cityGateAlt+"/"+cityGateBed);
        {
            int mx=CITY_CX*16+8,mz=CITY_CZ*16+8;
            require(polar.continentalIceThickness(mx,mz)>600&&polar.sample(mx,mz).height()>900&&bed.sample(mx,mz).height()<384,"The saved city start chunk lies on a deep sheet over a low bed");
            require(!DeepDarkHabitat.eligibleColumn(SEED,mx,mz,polar),"The saved city start column is no longer deep-dark eligible");
            require(!DeepDarkHabitat.contains(SEED,mx,DeepDarkHabitat.proposalY(polar.bedHeight(mx,mz)),mz,polar),"The city first gate rejects that start");
        }
        // Arithmetic only: the dripstone window (48..320 m) and sulfur window (96..384 m) are measured from the rock
        // datum, so for a sheet of at least the window depth the overburden-relative window would lie wholly in ice.
        // This does not call the production label or formation code; PolarHabitatChecks (native) does.
        int insideIce=0,thick=0;
        for(int[] site:SITES)for(int dz=-512;dz<=512;dz+=32)for(int dx=-512;dx<=512;dx+=32){
            int x=site[0]+dx,z=site[1]+dz,ice=polar.continentalIceThickness(x,z);if(ice<DripstoneHabitat.MAX_DEPTH)continue;
            thick++;int surface=polar.sample(x,z).height(),rock=polar.bedHeight(x,z);
            if(surface-DripstoneHabitat.MAX_DEPTH>rock)insideIce++;
            require(rock-DripstoneHabitat.MIN_DEPTH>TerrainShape.FLOOR+32,"The rock-relative window stays inside the terrain envelope");
        }
        require(thick>20&&insideIce==thick,"Dripstone-depth arithmetic: window wholly in ice for every sheet of at least 320 m: "+insideIce+"/"+thick);
        // Positive eligibility: a sheet over a bed above 384 m must keep the mask the ice-free bed
        // gives it (the negative checks above could pass if everything were ineligible).
        int highBedSheet=0,highBedEligible=0;
        for(int z=-2000000;z<=2000000;z+=16000)for(int x=-2000000;x<=2000000;x+=16000){
            if(polar.continentalIceThickness(x,z)==0||polar.bedHeight(x,z)<=384)continue;
            highBedSheet++;
            boolean e=DeepDarkHabitat.eligibleColumn(SEED,x,z,polar);if(e)highBedEligible++;
            require(e==DeepDarkHabitat.eligibleColumn(SEED,x,z,bed),"Sheet over a high bed keeps bed eligibility at "+x+","+z);
        }
        // Sheet thickness split of the 16 km survey grid (the denominator behind the dripstone/sulfur window statements).
        int[] split=new int[4];long iceSum=0;int iceMax=0;
        for(int z=-2000000;z<=2000000;z+=16000)for(int x=-2000000;x<=2000000;x+=16000){int t=polar.continentalIceThickness(x,z);if(t==0)continue;split[t<48?0:t<320?1:t<384?2:3]++;iceSum+=t;iceMax=Math.max(iceMax,t);}
        require(split[0]+split[1]+split[2]+split[3]>0,"Nonvacuous sheet thickness survey");
        // Bounded evidence for the ore-index fallback: rock relief per chunk stays far below the
        // ~800 m at which OreLayer.bind stops indexing (native repro covers the mechanism itself).
        int maxRelief=0;
        for(int[] site:SITES)for(int cz=(site[1]>>4)-RADIUS;cz<=(site[1]>>4)+RADIUS;cz++)for(int cx=(site[0]>>4)-RADIUS;cx<=(site[0]>>4)+RADIUS;cx++){
            int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;
            for(int z=0;z<16;z+=3)for(int x=0;x<16;x+=3){int h=polar.bedHeight(cx*16+x,cz*16+z);lo=Math.min(lo,h);hi=Math.max(hi,h);}
            maxRelief=Math.max(maxRelief,hi-lo);
        }
        require(maxRelief<800,"Measured per-chunk rock relief stays below the ore-index limit: "+maxRelief);
        // Cave approaches never start on, or run under, a sheet: an entrance needs an exposed surface.
        int cells=0,present=0,survey=0;
        for(int[] site:SITES)for(int cz=(site[1]>>4)/24-2;cz<=(site[1]>>4)/24+2;cz++)for(int cx=(site[0]>>4)/24-2;cx<=(site[0]>>4)/24+2;cx++){
            cells++;var found=CaveApproach.eligibleSite(SEED,cx,cz,polar,(x,z)->false);if(!found.present())continue;survey++;
            for(int z=cz*CaveApproach.CELL;z<cz*CaveApproach.CELL+CaveApproach.CELL;z+=4)for(int x=cx*CaveApproach.CELL;x<cx*CaveApproach.CELL+CaveApproach.CELL;x+=4)
                if(found.affectsColumn(x,z)){present++;require(polar.continentalIceThickness(x,z)==0,"Approach column under a sheet at "+x+","+z);}
        }
        // Full approach planning, walking route and entry continuation in and around sheet cells. Approaches
        // plan nothing on or under a sheet; a continuation that reaches rock beneath one reads the ice-free bed,
        // so it is identical to the one planned against the ice-free reference and lies below that bed.
        var field=new UndergroundStructure(SEED);var cellKeys=new java.util.TreeSet<Long>();
        for(int[] site:SITES)for(int cz=Math.floorDiv(site[1],384)-3;cz<=Math.floorDiv(site[1],384)+3;cz++)for(int cx=Math.floorDiv(site[0],384)-3;cx<=Math.floorDiv(site[0],384)+3;cx++)cellKeys.add(((long)cx<<32)|(cz&0xffffffffL));
        for(int z=-2000000;z<=2000000;z+=16000)for(int x=-2000000;x<=2000000;x+=16000)if(polar.continentalIceThickness(x,z)>0)
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)cellKeys.add(((long)(Math.floorDiv(x,384)+dx)<<32)|((Math.floorDiv(z,384)+dz)&0xffffffffL));
        int planned=0,sheetCells=0,presentApproaches=0,walkPoints=0,entries=0,entryPoints=0,entryUnderSheet=0;
        for(long key:cellKeys){
            int cx=(int)(key>>32),cz=(int)key;boolean sheetCell=false;
            for(int z=cz*384;z<cz*384+384&&!sheetCell;z+=32)for(int x=cx*384;x<cx*384+384;x+=32)if(polar.continentalIceThickness(x,z)>0){sheetCell=true;break;}
            if(sheetCell)sheetCells++;
            planned++;var approach=CaveApproach.plan(SEED,cx,cz,polar,(x,z)->false);if(!approach.present())continue;presentApproaches++;
            for(var p:approach.walkingRoute(polar,field)){walkPoints++;require(polar.continentalIceThickness(p[0],p[2])==0,"Approach route under a sheet at "+p[0]+","+p[2]);}
            var extended=approach.extend(polar,field);if(!extended.present())continue;entries++;
            require(java.util.Arrays.equals(extended.entry().encode(),CaveEntryRoute.plan(approach,bed,field).encode()),"Entry continuation equals the ice-free reference in cell "+cx+","+cz);
            for(var p:extended.entry().route()){
                entryPoints++;int rockTop=polar.bedHeight(p[0],p[2]);if(polar.continentalIceThickness(p[0],p[2])>0)entryUnderSheet++;
                require(p[1]<=rockTop-16&&field.voidAt(p[0],p[1],p[2],rockTop)&&field.voidAt(p[0],p[1]+1,p[2],rockTop),"Entry continuation lies in rock air below the bed at "+p[0]+","+p[1]+","+p[2]);
            }
        }
        require(sheetCells>0&&presentApproaches>0&&entries>0,"Nonvacuous approach/route planning: sheet cells "+sheetCells+" approaches "+presentApproaches+" entries "+entries);
        System.out.println("PASS polar underground datum: sheet columns on the 16 km grid by thickness <48/48-319/320-383/>=384 m = "+split[0]+"/"+split[1]+"/"+split[2]+"/"+split[3]+" (mean "+iceSum/Math.max(1,split[0]+split[1]+split[2]+split[3])+" m, max "+iceMax+"); max sampled chunk rock relief "+maxRelief+" m, "+insideIce+"/"+thick+" sheets of at least 320 m keep the dripstone window wholly in ice (arithmetic only), "+highBedSheet+" surveyed sheet columns over a bed above 384 m ("+highBedEligible+" eligible), "+checked+" datum points ("+sheetPoints+" under a sheet), "+sheetChunks+" sheet-bearing chunks, deep-dark masks identical to the ice-free bed ("+maskColumnsAlt+" eligible columns), "+survey+" surveyed approaches/"+present+" affected columns, none under a sheet; approach planning "+planned+" cells ("+sheetCells+" with a sheet): "+presentApproaches+" approaches, "+walkPoints+" walking-route points, "+entries+" entry continuations equal to the ice-free reference, "+entryPoints+" entry points ("+entryUnderSheet+" under a sheet)");
    }
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private PolarUndergroundCheck(){}
}
