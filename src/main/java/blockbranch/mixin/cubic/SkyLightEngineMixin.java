package blockbranch.mixin.cubic;

import blockbranch.cubic.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.lighting.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

/** Preserve vanilla propagation rules while jumping over sections without light data. */
@Mixin(SkyLightEngine.class)
abstract class SkyLightEngineMixin extends LightEngine {
    protected SkyLightEngineMixin(LightChunkGetter getter,LayerLightSectionStorage storage) { super(getter,storage); }
    @Shadow private int getLowestSourceY(int x,int z,int fallback) { throw new AssertionError(); }
    @Shadow private ChunkSkyLightSources getChunkSources(int x,int z) { throw new AssertionError(); }
    @Shadow @Final private ChunkSkyLightSources emptyChunkSources;
    @Shadow private static boolean crossedSectionEdge(Direction direction,int x,int z) { throw new AssertionError(); }
    @Unique private boolean blockbranch$expanded() { return ColumnLimits.expanded(chunkSource.getLevel()); }
    @Unique private LightStorageAccessor blockbranch$access() { return (LightStorageAccessor)storage; }
    @Unique private java.util.NavigableSet<Integer> blockbranch$layers(int x,int z) {
        return ((LightColumnIndex)blockbranch$access().blockbranch$updating()).blockbranch$sectionYs(SectionPos.getZeroNode(x,z));
    }
    @Inject(method="countEmptySectionsBelowIfAtBorder",at=@At("HEAD"),cancellable=true)
    private void blockbranch$count(long node,CallbackInfoReturnable<Integer> cir) {
        if(!blockbranch$expanded()) return;
        int x=BlockPos.getX(node),y=BlockPos.getY(node),z=BlockPos.getZ(node);
        if((y&15)!=0 || ((x&15)!=0 && (x&15)!=15 && (z&15)!=0 && (z&15)!=15)) { cir.setReturnValue(0); return; }
        Integer below=blockbranch$layers(x>>4,z>>4).lower(y>>4);
        int stop=below==null ? ((SkyStorageAccessor)storage).blockbranch$bottom()-1 : below;
        cir.setReturnValue(Math.max(0,(y>>4)-stop-1));
    }
    @Inject(method="propagateFromEmptySections",at=@At("HEAD"),cancellable=true)
    private void blockbranch$gaps(long node,Direction direction,int level,boolean increase,int empty,CallbackInfo ci) {
        if(!blockbranch$expanded()) return;
        ci.cancel();
        int x=BlockPos.getX(node),z=BlockPos.getZ(node);
        if(empty==0 || !crossedSectionEdge(direction,x&15,z&15)) return;
        int top=(BlockPos.getY(node)>>4)-1,bottom=top-empty+1;
        var access=blockbranch$access();
        for(int section:blockbranch$layers(x>>4,z>>4).subSet(bottom,true,top,true).descendingSet()) for(int y=15;y>=0;y--) {
            long target=BlockPos.asLong(x,(section<<4)+y,z);
            access.blockbranch$setLevel(target,increase ? level : 0);
            if(increase) { if(level>1) enqueueIncrease(target,QueueEntry.increaseSkipOneDirection(level,true,direction.getOpposite())); }
            else enqueueDecrease(target,QueueEntry.decreaseSkipOneDirection(level,direction.getOpposite()));
        }
    }
    @Inject(method="removeSourcesBelow",at=@At("HEAD"),cancellable=true)
    private void blockbranch$remove(int x,int z,int lowest,int worldBottom,CallbackInfo ci) {
        if(!blockbranch$expanded()) return;
        ci.cancel(); if(lowest<=worldBottom) return;
        int start=lowest-1; var access=blockbranch$access();
        for(int section:blockbranch$layers(x>>4,z>>4).headSet(start>>4,true).descendingSet()) {
            for(int y=Math.min((section<<4)+15,start);y>=(section<<4);y--) {
                long node=BlockPos.asLong(x,y,z);
                if(access.blockbranch$getLevel(node)!=15) return;
                access.blockbranch$setLevel(node,0);
                enqueueDecrease(node,y==lowest-1 ? QueueEntry.decreaseAllDirections(15) : QueueEntry.decreaseSkipOneDirection(15,Direction.UP));
            }
        }
    }
    @Inject(method="addSourcesAbove",at=@At("HEAD"),cancellable=true)
    private void blockbranch$add(int x,int z,int lowest,int worldBottom,CallbackInfo ci) {
        if(!blockbranch$expanded()) return;
        ci.cancel(); var access=blockbranch$access(); int start=Math.max(lowest,worldBottom);
        int neighbors=Math.max(Math.max(getLowestSourceY(x-1,z,Integer.MIN_VALUE),getLowestSourceY(x+1,z,Integer.MIN_VALUE)),Math.max(getLowestSourceY(x,z-1,Integer.MIN_VALUE),getLowestSourceY(x,z+1,Integer.MIN_VALUE)));
        for(int section:blockbranch$layers(x>>4,z>>4).tailSet(start>>4,true)) {
            for(int y=Math.max(section<<4,start);y<=(section<<4)+15;y++) {
                long node=BlockPos.asLong(x,y,z);
                if(access.blockbranch$getLevel(node)==15) return;
                access.blockbranch$setLevel(node,15);
                if(y<neighbors || y==lowest) enqueueIncrease(node,QueueEntry.increaseSkipOneDirection(15,false,Direction.UP));
            }
        }
    }
    @Inject(method="setLightEnabled",at=@At("HEAD"),cancellable=true)
    private void blockbranch$enable(ChunkPos pos,boolean enabled,CallbackInfo ci) {
        if(!blockbranch$expanded()) return;
        ci.cancel(); super.setLightEnabled(pos,enabled); if(!enabled) return;
        var sources=Objects.requireNonNullElse(getChunkSources(pos.x(),pos.z()),emptyChunkSources);
        int highest=sources.getHighestLowestSourceY()-1;
        int bottom=Math.max(((SkyStorageAccessor)storage).blockbranch$bottom(),(highest>>4)+1);
        for(int section:blockbranch$layers(pos.x(),pos.z()).tailSet(bottom,true)) {
            var data=blockbranch$access().blockbranch$writeLayer(SectionPos.asLong(pos.x(),section,pos.z()));
            if(data!=null && data.isEmpty()) data.fill(15);
        }
    }
    @Inject(method="propagateLightSources",at=@At("HEAD"),cancellable=true)
    private void blockbranch$propagate(ChunkPos pos,CallbackInfo ci) {
        if(!blockbranch$expanded()) return;
        ci.cancel(); super.setLightEnabled(pos,true);
        var access=blockbranch$access();
        var sources=Objects.requireNonNullElse(getChunkSources(pos.x(),pos.z()),emptyChunkSources);
        var north=Objects.requireNonNullElse(getChunkSources(pos.x(),pos.z()-1),emptyChunkSources);
        var south=Objects.requireNonNullElse(getChunkSources(pos.x(),pos.z()+1),emptyChunkSources);
        var west=Objects.requireNonNullElse(getChunkSources(pos.x()-1,pos.z()),emptyChunkSources);
        var east=Objects.requireNonNullElse(getChunkSources(pos.x()+1,pos.z()),emptyChunkSources);
        for(int section:blockbranch$layers(pos.x(),pos.z()).descendingSet()) {
            var data=access.blockbranch$writeLayer(SectionPos.asLong(pos.x(),section,pos.z()));
            int min=section<<4,max=min+15; boolean below=false;
            for(int z=0;z<16;z++) for(int x=0;x<16;x++) {
                int lowest=sources.getLowestSourceY(x,z); if(lowest>max) continue;
                int n=z==0 ? north.getLowestSourceY(x,15) : sources.getLowestSourceY(x,z-1);
                int s=z==15 ? south.getLowestSourceY(x,0) : sources.getLowestSourceY(x,z+1);
                int w=x==0 ? west.getLowestSourceY(15,z) : sources.getLowestSourceY(x-1,z);
                int e=x==15 ? east.getLowestSourceY(0,z) : sources.getLowestSourceY(x+1,z);
                int neighbors=Math.max(Math.max(n,s),Math.max(w,e));
                for(int y=max;y>=Math.max(min,lowest);y--) {
                    data.set(x,y&15,z,15);
                    if(y==lowest || y<neighbors) enqueueIncrease(BlockPos.asLong(pos.getMinBlockX()+x,y,pos.getMinBlockZ()+z),QueueEntry.increaseSkySourceInDirections(y==lowest,y<n,y<s,y<w,y<e));
                }
                below|=lowest<min;
            }
            if(!below) break;
        }
    }
}
