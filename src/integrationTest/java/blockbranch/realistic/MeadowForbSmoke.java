package blockbranch.realistic;

import blockbranch.Blockbranch;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.RandomState;

/** Photographic geometry chosen without reading flower roles or plant density. */
public final class MeadowForbSmoke {
    private static final Map<Long,BlockPos> anchors=new HashMap<>();
    public static BlockPos anchor(RealisticGenerator generator,RandomState random){
        String fixed=System.getProperty("blockbranch.meadowForbCapturePoint","");
        if(!fixed.isBlank()){var xyz=fixed.split(",");if(xyz.length!=3)throw new IllegalArgumentException("meadowForbCapturePoint requires x,y,z");return new BlockPos(Integer.parseInt(xyz[0]),Integer.parseInt(xyz[1]),Integer.parseInt(xyz[2]));}
        return anchors.computeIfAbsent(random.seed(),seed->choose(generator,random));
    }
    private static BlockPos choose(RealisticGenerator g,RandomState random){
        var start=MeadowSmoke.fixture(g,random);var resolver=g.getBiomeSource().createUncachedResolver(random);double best=0;int checked=0;
        for(int radius=0;radius<=2048;radius+=64)for(int dz=-radius;dz<=radius;dz+=64)for(int dx=-radius;dx<=radius;dx+=64){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;int x=start.getX()+dx,z=start.getZ()+dz;
            if(!resolver.getNoiseBiome(x>>2,0,z>>2).is(Blockbranch.id("rolling_lowland_forb_grass_meadow")))continue;
            int rough=0;for(int oz=-128;oz<=128;oz+=32)for(int ox=-128;ox<=128;ox+=32)if(resolver.getNoiseBiome((x+ox)>>2,0,(z+oz)>>2).is(Blockbranch.id("rolling_lowland_forb_grass_meadow")))rough++;
            if(rough<69)continue;checked++;int actual=0,clear=0;
            for(int oz=-128;oz<=128;oz+=8)for(int ox=-128;ox<=128;ox+=8)if(resolver.getNoiseBiome((x+ox)>>2,0,(z+oz)>>2).is(Blockbranch.id("rolling_lowland_forb_grass_meadow")))actual++;
            best=Math.max(best,actual/1089.0);if(actual<981)continue;
            for(int oz=-128;oz<=128;oz+=16)for(int ox=-128;ox<=128;ox+=16)if(g.canopy(x+ox,z+oz,random).topY(x+ox,z+oz)==Integer.MIN_VALUE)clear++;
            if(clear<261)continue;
            boolean lens=true;for(int oz=-1;oz<=1;oz++)for(int ox=-1;ox<=1;ox++)if(g.canopy(x+ox,z+oz,random).topY(x+ox,z+oz)!=Integer.MIN_VALUE)lens=false;
            if(!lens)continue;var result=new BlockPos(x,g.drainage(random).sample(x,z).height(),z);
            Blockbranch.LOGGER.info("MEADOW_FORB_GEOMETRY_ANCHOR anchor={},{},{} actualMeadow={}/1089 canopyFree={}/289 radius={} candidates={} flowerCriterion=false",x,result.getY(),z,actual,clear,radius,checked);return result;
        }
        throw new AssertionError("No representative meadow interior within2048; bestActualCoverage="+best+" fineCandidates="+checked);
    }
    public static void capture(net.minecraft.server.level.ServerPlayer player,int stage){
        var level=player.level();var server=level.getServer();var g=(RealisticGenerator)level.getChunkSource().getGenerator();var p=anchor(g,level.getChunkSource().randomState());boolean overlook=stage==126;int h=p.getY();
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)h=Math.max(h,g.drainage(level.getChunkSource().randomState()).sample(p.getX()+dx,p.getZ()+dz).height());
        int y=overlook?p.getY()+30:h+2;level.getGameRules().set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED,0,server);player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"time set 6000");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" "+(p.getX()+.5)+" "+y+" "+(p.getZ()+.5)+" 45 "+(overlook?35:10));
        Blockbranch.LOGGER.info("MEADOW_FORB_FIXED_CAMERA stage={} anchor={},{},{} feetY={} yaw=45 pitch={} nineFeetColumnsChecked=true",stage,p.getX(),p.getY(),p.getZ(),y,overlook?35:10);
    }
    private MeadowForbSmoke(){}
}
