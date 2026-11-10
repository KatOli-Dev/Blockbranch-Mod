package blockbranch.realistic;

import blockbranch.Blockbranch;
import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.RandomState;

/** Native old-growth identities, supported canopy, persistence and honest clear-lens views. */
public final class OldGrowthTaigaSmoke {
    private static final Map<DrainageNetwork,BlockPos[]> FIXTURES=Collections.synchronizedMap(new WeakHashMap<>());
    public static BlockPos fixture(RealisticGenerator g,RandomState r,int kind){
        var d=g.drainage(r);synchronized(FIXTURES){
            var pair=FIXTURES.computeIfAbsent(d,unused->new BlockPos[2]);
            if(pair[kind-1]!=null)return pair[kind-1];
            String requested=System.getProperty("blockbranch.oldGrowthCapturePoint");
            if(requested!=null){
                String[] xy=requested.split(",",-1);require(xy.length==2,"Matched capture point requires x,z");
                int x=Integer.parseInt(xy[0]),z=Integer.parseInt(xy[1]);var c=new ClimateField(r.seed());
                require(OldGrowthTaiga.kind(r.seed(),x,z,d,c)==kind,"Matched capture biome must remain unchanged");
                require(BorealTrees.roots(r.seed(),x-8,z-8,x+8,z+8,d,c).stream().anyMatch(root->root.x()==x&&root.z()==z&&root.family()==OldGrowthTaiga.family(kind)),"Matched capture must retain its natural stem");
                var bed=new BlockPos(x,d.sample(x,z).height(),z);require(clearMatchedEyes(g,r,bed),"Original matched camera eye is now obstructed");
                pair[kind-1]=bed;return bed;
            }
            var p=blockbranch.realistic.OldGrowthTaigaFixtures.fixture(d,r.seed(),kind);var c=new ClimateField(r.seed());
            for(var root:BorealTrees.roots(r.seed(),p.x()-96,p.z()-96,p.x()+96,p.z()+96,d,c)){
                if(root.family()!=OldGrowthTaiga.family(kind)||root.height()<26||OldGrowthTaiga.kind(r.seed(),root.x(),root.z(),d,c)!=kind)continue;
                var bed=new BlockPos(root.x(),root.y()-1,root.z());
                if(clearViews(g,r,bed)){pair[kind-1]=bed;return bed;}
            }
            throw new AssertionError("No unobstructed natural old-growth views kind="+kind+" anchor="+p);
        }
    }
    private static boolean clearMatchedEyes(RealisticGenerator g,RandomState r,BlockPos bed){
        // A matching pose must not be relocated just because nearby foliage now
        // enters the default search's padding. The actual camera eye stays clear;
        // the client independently checks its live eye position before capture.
        for(boolean elevated:new boolean[]{false,true}){
            var view=blockbranch.realistic.OldGrowthTaigaFixtures.view(bed.getX(),bed.getY(),bed.getZ(),elevated);
            var eye=BlockPos.containing(view.x(),view.y()+1.62,view.z());
            if(!g.surfaceState(eye.getX(),eye.getY(),eye.getZ(),r).isAir())return false;
        }
        return true;
    }
    private static boolean clearViews(RealisticGenerator g,RandomState r,BlockPos bed){
        var d=g.drainage(r);
        for(boolean elevated:new boolean[]{false,true}){
            var view=blockbranch.realistic.OldGrowthTaigaFixtures.view(bed.getX(),bed.getY(),bed.getZ(),elevated);
            if(!elevated&&Math.abs(d.sample((int)Math.floor(view.x()),(int)Math.floor(view.z())).height()-bed.getY())>1)return false;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(int dy=0;dy<=3;dy++)
                if(!g.surfaceState((int)Math.floor(view.x())+dx,(int)Math.floor(view.y())+dy,(int)Math.floor(view.z())+dz,r).isAir())return false;
        }
        return true;
    }
    public static void verify(ServerLevel level,RealisticGenerator g,boolean reload){
        var r=level.getChunkSource().randomState();
        for(int kind:new int[]{OldGrowthTaiga.PINE,OldGrowthTaiga.SPRUCE}){
            var bed=fixture(g,r,kind);var expected=kind==OldGrowthTaiga.PINE?Biomes.OLD_GROWTH_PINE_TAIGA:Biomes.OLD_GROWTH_SPRUCE_TAIGA;
            TaigaSmoke.verifyAt(level,g,reload,bed,expected,kind==OldGrowthTaiga.PINE?"OLD_GROWTH_PINE":"OLD_GROWTH_SPRUCE");
            var resolver=g.getBiomeSource().createUncachedResolver(r);
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
                var chunk=level.getChunk((bed.getX()>>4)+dx,(bed.getZ()>>4)+dz);var base=((SparseColumn)chunk).blockbranch$terrain().base();var copy=TerrainProfile.decode(base.encode(level.palettedContainerFactory()),level.palettedContainerFactory());
                for(int z=0;z<16;z+=4)for(int x=0;x<16;x+=4)for(int y:new int[]{-3000,0,2000}){
                    int wx=chunk.getPos().getMinBlockX()+x,wz=chunk.getPos().getMinBlockZ()+z;var biome=resolver.getNoiseBiome(wx>>2,y,wz>>2);
                    require(chunk.getNoiseBiome(wx>>2,y,wz>>2).equals(biome)&&copy.biomes().get(x>>2,y&3,z>>2).equals(biome),"Stored/source/decoded old-growth identity");
                }
            }
            Blockbranch.LOGGER.info("OLD_GROWTH_VIEW_WITNESS kind={} bed={} ground={} elevated={}",kind,bed,blockbranch.realistic.OldGrowthTaigaFixtures.view(bed.getX(),bed.getY(),bed.getZ(),false),blockbranch.realistic.OldGrowthTaigaFixtures.view(bed.getX(),bed.getY(),bed.getZ(),true));
        }
    }
    public static void probe(ServerPlayer player,int stage){
        var level=player.level();var g=(RealisticGenerator)level.getChunkSource().getGenerator();boolean spruce=Boolean.getBoolean("blockbranch.oldGrowthSpruceClient");var bed=fixture(g,level.getChunkSource().randomState(),spruce?2:1);var test=bed.above(40).east();
        if(stage<=92){
            level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,level.getServer());player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"time set 6000");
            var view=stage==90?new OldGrowthTaiga.View(bed.getX()+.5,bed.getY()+40,bed.getZ()+.5,0,30):blockbranch.realistic.OldGrowthTaigaFixtures.view(bed.getX(),bed.getY(),bed.getZ(),stage==92);
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),"tp "+player.getName().getString()+" "+view.x()+" "+view.y()+" "+view.z()+" "+view.yaw()+" "+view.pitch());
            Blockbranch.LOGGER.info("OLD_GROWTH_CAPTURE_CAMERA spruce={} stage={} bed={} view={}",spruce,stage,bed,view);
        }else if(stage==93)level.setBlock(test,Blocks.SPRUCE_LEAVES.defaultBlockState().setValue(LeavesBlock.DISTANCE,7).setValue(LeavesBlock.PERSISTENT,false),2);
        else{var base=((SparseColumn)level.getChunkAt(test)).blockbranch$terrain().base();level.setBlock(test,base.state(test.getX(),test.getY(),test.getZ()),2);}
    }
    private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    private OldGrowthTaigaSmoke(){}
}
