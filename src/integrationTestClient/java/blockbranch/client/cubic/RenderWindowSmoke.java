package blockbranch.client.cubic;

import blockbranch.Blockbranch;
import blockbranch.cubic.ColumnLimits;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.RotatingSectionStorage;
import net.minecraft.core.SectionPos;
import java.nio.file.Files;
import java.nio.file.Path;

/** Opt-in regression checks against the actual mixed-in vanilla ring storage. */
public final class RenderWindowSmoke {
    private static final class Value implements RotatingSectionStorage.Value {
        private long node;
        private int moves;
        Value(long node) { this.node=node; }
        public void setSectionNode(long node) { this.node=node; moves++; }
        public long getSectionNode() { return node; }
    }
    public static void register() {
        try { Files.deleteIfExists(Path.of(System.getProperty("blockbranch.renderSmokeResult","/tmp/column-render-pass"))); }
        catch(java.io.IOException e) { throw new RuntimeException(e); }
        for(int radius:new int[]{2,8,16}) {
            var storage=new RotatingSectionStorage<Value>(radius,ColumnLimits.MIN_Y>>4,(ColumnLimits.MAX_Y>>4)-1,(i,node)->new Value(node));
            for(int y:new int[]{0,-40,10000,(ColumnLimits.MIN_Y>>4),(ColumnLimits.MAX_Y>>4)-1}) {
                storage.repositionCenter(SectionPos.of(-3,y,5));
                check(storage.height()==2*(radius+1)+1,"Bounded height");
                for(int delta:new int[]{1,-1,1}) {
                    int oldMin=storage.minY(),oldMax=storage.maxY();
                    var old=storage.getValue(-3,Math.clamp(y,oldMin,oldMax),5);
                    storage.forEach(v->v.moves=0);
                    y=Math.clamp(y+delta,ColumnLimits.MIN_Y>>4,(ColumnLimits.MAX_Y>>4)-1);
                    storage.repositionCenter(SectionPos.of(-3,y,5));
                    int[] moves={0}; storage.forEach(v->moves[0]+=v.moves);
                    int width=2*radius+1;
                    check(moves[0]==Math.abs(storage.minY()-oldMin)*width*width,"Only entering Y layers recycled");
                    int retainedY=SectionPos.y(old.node);
                    if(retainedY>=storage.minY() && retainedY<=storage.maxY()) check(storage.getValue(-3,retainedY,5)==old,"Retained slot identity");
                    verify(storage,-3,5,radius);
                }
                // Simultaneous horizontal and vertical movement must use the same mapping.
                storage.repositionCenter(SectionPos.of(-2,y+1,4));
                verify(storage,-2,4,radius);
            }
        }
        var normal=new RotatingSectionStorage<Value>(2,0,15,(i,node)->new Value(node));
        normal.repositionCenter(SectionPos.of(0,8,0)); normal.forEach(v->v.moves=0);
        normal.repositionCenter(SectionPos.of(0,9,0));
        normal.forEach(v->check(v.moves==0,"Vanilla dimension Y slots unchanged"));
        Blockbranch.LOGGER.info("COLUMN_RENDER_WINDOW_PASS radii=2,8,16; signed Y, edges, retained meshes, simultaneous X/Y/Z movement");
        ClientTickEvents.END_CLIENT_TICK.register(new net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick() {
            int ticks;
            int moves;
            int targetY;
            int settled;
            long requestedAt;
            boolean started;
            public void onEndTick(net.minecraft.client.Minecraft client) {
                if(client.level==null || client.player==null) return;
                if(++ticks<100) return;
                if(!started) {
                    command(client,"gamemode spectator "+client.player.getName().getString());
                    started=true;
                }
                if(requestedAt!=0) {
                    if(Math.abs(client.player.getY()-targetY)>=0.1) {
                        check(System.nanoTime()-requestedAt<30_000_000_000L,"Vertical teleport timed out");
                        return;
                    }
                    if(++settled<20) return;
                    requestedAt=0;
                    moves++;
                }
                if(moves<16) {
                    targetY=96+16*(moves%4);
                    settled=0;
                    requestedAt=System.nanoTime();
                    command(client,"tp "+client.player.getName().getString()+" 1 "+targetY+" 1");
                    return;
                }
                try { Files.writeString(Path.of(System.getProperty("blockbranch.renderSmokeResult","/tmp/column-render-pass")),"PASS\n"); }
                catch(java.io.IOException e) { throw new RuntimeException(e); }
                Blockbranch.LOGGER.info("COLUMN_RENDER_MOVEMENT_PASS crossings={} framesPerSecond={}",moves,client.getFps());
                client.stop();
            }
        });
    }
    private static void command(net.minecraft.client.Minecraft client,String command) {
        var server=client.getSingleplayerServer();
        if(server==null) throw new IllegalStateException("Render movement smoke requires an integrated server");
        server.execute(()->server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),command));
    }
    private static void verify(RotatingSectionStorage<Value> storage,int cx,int cz,int radius) {
        for(int x=cx-radius;x<=cx+radius;x++) for(int z=cz-radius;z<=cz+radius;z++) for(int y=storage.minY();y<=storage.maxY();y++) {
            var value=storage.getValue(x,y,z);
            check(value!=null && value.getSectionNode()==SectionPos.asLong(x,y,z),"Absolute slot lookup");
        }
        check(storage.getValue(cx,storage.minY()-1,cz)==null,"Lower window bound");
        check(storage.getValue(cx,storage.maxY()+1,cz)==null,"Upper window bound");
    }
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private RenderWindowSmoke() {}
}
