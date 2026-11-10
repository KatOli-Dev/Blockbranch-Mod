package blockbranch.realistic;

import java.util.function.Predicate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Versioned, bounded local proposals over surviving host rock. No full-height storage or global cache. */
public final class OreLayer {
    public static final OreLayer NONE=new OreLayer();
    public static final int DEFINITION=12, MIN_DEPTH=4, MAX_DEPTH=224;
    public enum Kind {
        COAL(4,40,144,.50,2.2,Blocks.COAL_ORE.defaultBlockState(),Blocks.DEEPSLATE_COAL_ORE.defaultBlockState()),
        IRON(8,56,144,.75,1.8,Blocks.IRON_ORE.defaultBlockState(),Blocks.DEEPSLATE_IRON_ORE.defaultBlockState()),
        COPPER(8,40,160,.60,2.0,Blocks.COPPER_ORE.defaultBlockState(),Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState()),
        GOLD(56,112,224,.20,1.7,Blocks.GOLD_ORE.defaultBlockState(),Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState()),
        REDSTONE(64,144,224,.65,1.7,Blocks.REDSTONE_ORE.defaultBlockState(),Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState()),
        LAPIS(40,104,224,.22,1.6,Blocks.LAPIS_ORE.defaultBlockState(),Blocks.DEEPSLATE_LAPIS_ORE.defaultBlockState()),
        DIAMOND(96,144,208,.65,1.35,Blocks.DIAMOND_ORE.defaultBlockState(),Blocks.DEEPSLATE_DIAMOND_ORE.defaultBlockState()),
        EMERALD(8,32,112,.10,1.2,Blocks.EMERALD_ORE.defaultBlockState(),Blocks.DEEPSLATE_EMERALD_ORE.defaultBlockState());
        public final int min,peak,max;public final double activation,radius;public final BlockState stone,deepslate;
        Kind(int min,int peak,int max,double activation,double radius,BlockState stone,BlockState deepslate){this.min=min;this.peak=peak;this.max=max;this.activation=activation;this.radius=radius;this.stone=stone;this.deepslate=deepslate;}
        double density(int depth){if(depth<min||depth>max)return 0;return depth<=peak?(depth-min)/(double)(peak-min):(max-depth)/(double)(max-peak);}
    }
    // Published definitions1–11 retain their placement rules. Append ores only under new definitions.
    private static final Kind[] KINDS={Kind.COAL,Kind.IRON,Kind.COPPER,Kind.GOLD,Kind.REDSTONE,Kind.LAPIS,Kind.DIAMOND,Kind.EMERALD};
    private final Index index;
    private final UndergroundStructure caveField;
    private final long seed;private final int cx,cz,definition,emeraldMask,goldMask,offsetX,offsetY,offsetZ;
    private OreLayer(){index=null;caveField=null;seed=0;cx=cz=definition=emeraldMask=goldMask=offsetX=offsetY=offsetZ=0;}
    public OreLayer(long seed,int cx,int cz,int definition,int emeraldMask,int goldMask){this(seed,cx,cz,definition,emeraldMask,goldMask,null);}
    private OreLayer(long seed,int cx,int cz,int definition,int emeraldMask,int goldMask,Index index){
        if((definition<1||definition>DEFINITION)||cx<(Integer.MIN_VALUE>>4)||cx>(Integer.MAX_VALUE>>4)||cz<(Integer.MIN_VALUE>>4)||cz>(Integer.MAX_VALUE>>4)||(emeraldMask&~65535)!=0||(goldMask&~65535)!=0)throw new IllegalArgumentException("Invalid ore descriptor");
        this.index=index;this.caveField=new UndergroundStructure(seed);this.seed=seed;this.cx=cx;this.cz=cz;this.definition=definition;this.emeraldMask=emeraldMask;this.goldMask=goldMask;long h=mix(seed^0x4f52455f7631L);offsetX=(int)h&7;offsetY=(int)(h>>>8)&7;offsetZ=(int)(h>>>16)&7;
    }
    public int definition(){return definition;}
    public boolean enabled(){return definition!=0;}
    public boolean owns(int x,int z){return !enabled()||x==cx&&z==cz;}
    public boolean mayAffect(int sy,int lowestGround,int highestGround){return enabled()&&((long)sy<<4)<=highestGround-MIN_DEPTH&&((long)sy<<4)+15>=lowestGround-MAX_DEPTH;}
    public BlockState state(int localX,int y,int localZ,int ground,BlockState host){
        return state(localX,y,localZ,ground,host,false);
    }
    public BlockState state(int localX,int y,int localZ,int ground,BlockState host,boolean caves){
        if(definition>=2){
            var existing=proposals(localX,y,localZ,ground,host,caves);
            if(definition>=3&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.TinVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.MetallurgyContent.DEEPSLATE_TIN_ORE:blockbranch.metallurgy.MetallurgyContent.TIN_ORE).defaultBlockState();
            if(definition>=4&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.ZincVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.ZincBrassContent.DEEPSLATE_ZINC_ORE:blockbranch.metallurgy.ZincBrassContent.ZINC_ORE).defaultBlockState();
            if(definition>=5&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.ManganeseVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.SteelContent.DEEPSLATE_MANGANESE_ORE:blockbranch.metallurgy.SteelContent.MANGANESE_ORE).defaultBlockState();
            if(definition>=6&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.TungstenVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.TungstenContent.DEEPSLATE_TUNGSTEN_ORE:blockbranch.metallurgy.TungstenContent.TUNGSTEN_ORE).defaultBlockState();
            if(definition>=7&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.NickelVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.StainlessContent.DEEPSLATE_NICKEL_ORE:blockbranch.metallurgy.StainlessContent.NICKEL_ORE).defaultBlockState();
            if(definition>=7&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.ChromiumVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.StainlessContent.DEEPSLATE_CHROMIUM_ORE:blockbranch.metallurgy.StainlessContent.CHROMIUM_ORE).defaultBlockState();
            if(definition>=8&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.LeadVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.LeadContent.DEEPSLATE_LEAD_ORE:blockbranch.metallurgy.LeadContent.LEAD_ORE).defaultBlockState();
            if(definition>=9&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.FoolsDiamondVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.FoolsDiamondContent.DEEPSLATE_ORE:blockbranch.metallurgy.FoolsDiamondContent.ORE).defaultBlockState();
            if(definition>=10&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.SilverVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.PreciousContent.DEEPSLATE_SILVER_ORE:blockbranch.metallurgy.PreciousContent.SILVER_ORE).defaultBlockState();
            if(definition>=10&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.PlatinumVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.PreciousContent.DEEPSLATE_PLATINUM_ORE:blockbranch.metallurgy.PreciousContent.PLATINUM_ORE).defaultBlockState();
            if(definition>=11&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.TitaniumVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.TitaniumContent.DEEPSLATE_ORE:blockbranch.metallurgy.TitaniumContent.ORE).defaultBlockState();
            if(definition>=12&&existing==host&&(host.is(Blocks.STONE)||host.is(Blocks.DEEPSLATE))
                &&blockbranch.metallurgy.MagnetiteVeins.contains(seed,(cx<<4)+(localX&15),y,(cz<<4)+(localZ&15),ground-y))
                return (host.is(Blocks.DEEPSLATE)?blockbranch.metallurgy.MagnetiteContent.DEEPSLATE_ORE:blockbranch.metallurgy.MagnetiteContent.ORE).defaultBlockState();
            return existing;
        }
        return frozenV1(localX,y,localZ,ground,host,caves);
    }
    private BlockState frozenV1(int localX,int y,int localZ,int ground,BlockState host,boolean caves){
        if(!enabled()||(!host.is(Blocks.STONE)&&!host.is(Blocks.DEEPSLATE)))return host;
        int depth=ground-y;if(depth<MIN_DEPTH||depth>MAX_DEPTH)return host;
        long x=((long)cx<<4)+(localX&15)+offsetX,z=((long)cz<<4)+(localZ&15)+offsetZ,py=(long)y+offsetY;
        long h=mix(seed^Math.floorDiv(x,8)*0x9E3779B97F4A7C15L^Math.floorDiv(py,8)*0xD6E8FEB86659FD93L^Math.floorDiv(z,8)*0xA24BAED4963EE407L);
        Kind kind=KINDS[(int)h&7];double density=kind.density(depth);
        int bit=1<<(((localZ&15)>>2)*4+((localX&15)>>2));if(kind==Kind.EMERALD&&(emeraldMask&bit)==0)return host;
        double chance=kind.activation*density;
        if(kind==Kind.GOLD&&(goldMask&bit)!=0&&depth>16&&depth<96)chance+=.85*(depth<24?(depth-16)/8.0:depth<=56?1:(96-depth)/40.0);
        if(chance<=0)return host;chance=Math.min(.95,chance);
        if(unit(h>>>8)>=chance)return host;
        long shape=mix(h^0x7665696eL);double rx=kind.radius*(.9+.2*unit(shape)),ry=kind.radius*(.60+.20*unit(shape>>>16)),rz=kind.radius*(.75+.20*unit(shape>>>32));
        double px=rx+.25+unit(mix(shape))*(7.5-2*rx),qy=ry+.25+unit(mix(shape^17))*(7.5-2*ry),qz=rz+.25+unit(mix(shape^37))*(7.5-2*rz);
        double dx=(Math.floorMod(x,8)+.5-px)/rx,dy=(Math.floorMod(py,8)+.5-qy)/ry,dz=(Math.floorMod(z,8)+.5-qz)/rz;
        if(dx*dx+dy*dy+dz*dz>=1)return host;
        // Match the native preference for buried valuables without consulting mutable
        // neighboring blocks. Unclipped seeded geometry is identical across chunk
        // seams and later mining cannot change this definition's ore assignment.
        // Native badlands extra gold has no exposure discard. Keep its shallow
        // regional bonus visible; ordinary gold and lapis retain a buried bias.
        double discard=kind==Kind.DIAMOND?.7:kind==Kind.GOLD?((goldMask&bit)!=0&&depth<96?0:.5):kind==Kind.LAPIS?.5:0;
        if(caves&&discard>0){
            int wx=(int)(x-offsetX),wz=(int)(z-offsetZ);
            long block=mix(seed^(long)wx*0x9E3779B97F4A7C15L^(long)y*0xD6E8FEB86659FD93L^(long)wz*0xA24BAED4963EE407L^0x627572696564L);
            if(unit(block)<discard&&(naturalVoid(wx+1,y,wz)||naturalVoid(wx-1,y,wz)||naturalVoid(wx,y+1,wz)||naturalVoid(wx,y-1,wz)||naturalVoid(wx,y,wz+1)||naturalVoid(wx,y,wz-1)))return host;
        }
        return host.is(Blocks.DEEPSLATE)?kind.deepslate:kind.stone;
    }
    /** Immutable per-profile index; the descriptor never serializes this derived cache. */
    public OreLayer bind(int lowestGround,int highestGround){
        if(definition<2)return this;
        int minX=(int)Math.floorDiv((long)cx*16+offsetX-3,8),maxX=(int)Math.floorDiv((long)cx*16+offsetX+18,8);
        int minZ=(int)Math.floorDiv((long)cz*16+offsetZ-3,8),maxZ=(int)Math.floorDiv((long)cz*16+offsetZ+18,8);
        int minY=(int)Math.floorDiv((long)lowestGround-MAX_DEPTH+offsetY-3,8),maxY=(int)Math.floorDiv((long)highestGround-MIN_DEPTH+offsetY+3,8);
        if(index!=null&&index.lowest==lowestGround&&index.highest==highestGround)return this;
        int nx=maxX-minX+1,ny=maxY-minY+1,nz=maxZ-minZ+1;
        if((long)nx*ny*nz>2048)return this; // bounded direct fallback, including unusual cliffs
        int tileY=(int)Math.floorDiv((long)lowestGround-MAX_DEPTH,4),tileEnd=(int)Math.floorDiv((long)highestGround-MIN_DEPTH,4),tileHeight=tileEnd-tileY+1;
        if((long)tileHeight*16>4096)return this;
        var values=new java.util.ArrayList<Proposal>();
        for(int x=minX;x<=maxX;x++)for(int y=minY;y<=maxY;y++)for(int z=minZ;z<=maxZ;z++){
            // Reject inactive cells before allocating/decoding their shapes.
            // The whole cell plus radius is deliberately broader than support.
            long h=mix(seed^(long)x*0x9E3779B97F4A7C15L^(long)y*0xD6E8FEB86659FD93L^(long)z*0xA24BAED4963EE407L);var k=KINDS[(int)h&7];
            if(k==Kind.EMERALD&&emeraldMask==0)continue;
            double cellLow=lowestGround-((double)y*8-offsetY+11),cellHigh=highestGround-((double)y*8-offsetY-3);
            double cellDensity=cellLow<=k.peak&&cellHigh>=k.peak?1:Math.max(density(k,cellLow),density(k,cellHigh));
            double cellUpper=k.activation*cellDensity;if(k==Kind.GOLD&&goldMask!=0&&cellLow<96&&cellHigh>16)cellUpper+=.85;
            if(unit(h>>>8)>=Math.min(.95,cellUpper))continue;
            var p=proposal(x,y,z);
            double low=lowestGround-(p.y+3),high=highestGround-(p.y-3);
            double density=low<=k.peak&&high>=k.peak?1:Math.max(density(k,low),density(k,high));
            double upper=k.activation*density;
            if(k==Kind.GOLD&&goldMask!=0&&low<96&&high>16)upper+=.85;
            if(p.draw>=Math.min(.95,upper)||p.x+3<(cx<<4)+.5||p.x-3>(cx<<4)+15.5||p.z+3<(cz<<4)+.5||p.z-3>(cz<<4)+15.5)continue;
            values.add(p);
        }
        int tiles=tileHeight*16;int[] offsets=new int[tiles+1];var ranges=new int[values.size()][];
        for(int i=0;i<values.size();i++){
            var p=values.get(i);double rx=Math.sqrt(1/p.invX)+1e-6,ry=Math.sqrt(1/p.invY)+1e-6,rz=Math.sqrt(1/p.invZ)+1e-6;
            int[] b={Math.max(0,(int)Math.floor((p.x-rx-(cx<<4)-.5)/4)),Math.min(3,(int)Math.floor((p.x+rx-(cx<<4)-.5)/4)),Math.max(0,(int)Math.floor((p.y-ry-.5)/4)-tileY),Math.min(tileHeight-1,(int)Math.floor((p.y+ry-.5)/4)-tileY),Math.max(0,(int)Math.floor((p.z-rz-(cz<<4)-.5)/4)),Math.min(3,(int)Math.floor((p.z+rz-(cz<<4)-.5)/4))};ranges[i]=b;
            for(int x=b[0];x<=b[1];x++)for(int y=b[2];y<=b[3];y++)for(int z=b[4];z<=b[5];z++)offsets[(x*tileHeight+y)*4+z+1]++;
        }
        for(int i=1;i<offsets.length;i++)offsets[i]+=offsets[i-1];int[] cursor=offsets.clone(),refs=new int[offsets[tiles]];
        for(int i=0;i<values.size();i++){int[] b=ranges[i];for(int x=b[0];x<=b[1];x++)for(int y=b[2];y<=b[3];y++)for(int z=b[4];z<=b[5];z++)refs[cursor[(x*tileHeight+y)*4+z]++]=i;}
        return new OreLayer(seed,cx,cz,definition,emeraldMask,goldMask,new Index(tileY,tileHeight,lowestGround,highestGround,offsets,refs,values.toArray(Proposal[]::new)));
    }
    private static double density(Kind k,double depth){if(depth<k.min||depth>k.max)return 0;return depth<=k.peak?(depth-k.min)/(k.peak-k.min):(k.max-depth)/(k.max-k.peak);}
    private record Index(int y,int ny,int lowest,int highest,int[] offsets,int[] refs,Proposal[] values){}
    private record Proposal(Kind kind,double x,double y,double z,double invX,double invY,double invZ,double draw){}
    private Proposal proposal(int x,int y,int z){
        long h=mix(seed^(long)x*0x9E3779B97F4A7C15L^(long)y*0xD6E8FEB86659FD93L^(long)z*0xA24BAED4963EE407L),shape=mix(h^0x7665696eL);var k=KINDS[(int)h&7];
        double rx=k.radius*(.9+.2*unit(shape)),ry=k.radius*(.60+.20*unit(shape>>>16)),rz=k.radius*(.75+.20*unit(shape>>>32));
        return new Proposal(k,(double)x*8-offsetX+8*unit(mix(shape)),(double)y*8-offsetY+8*unit(mix(shape^17)),(double)z*8-offsetZ+8*unit(mix(shape^37)),1/(rx*rx),1/(ry*ry),1/(rz*rz),unit(h>>>8));
    }
    private BlockState proposals(int lx,int y,int lz,int ground,BlockState host,boolean caves){
        if(!host.is(Blocks.STONE)&&!host.is(Blocks.DEEPSLATE))return host;
        int depth=ground-y;if(depth<MIN_DEPTH||depth>MAX_DEPTH)return host;
        int wx=(cx<<4)+(lx&15),wz=(cz<<4)+(lz&15),bit=1<<(((lz&15)>>2)*4+((lx&15)>>2));
        if(index!=null){
            int yy=Math.floorDiv(y,4)-index.y;if(yy<0||yy>=index.ny)return host;
            int tile=(((lx&15)>>2)*index.ny+yy)*4+((lz&15)>>2);
            for(int i=index.offsets[tile];i<index.offsets[tile+1];i++){var value=accepted(index.values[index.refs[i]],wx,y,wz,depth,bit,host,caves);if(value!=null)return value;}
            return host;
        }
        long xx=(long)wx+offsetX,yy=(long)y+offsetY,zz=(long)wz+offsetZ;
        int gx=(int)Math.floorDiv(xx,8),gy=(int)Math.floorDiv(yy,8),gz=(int)Math.floorDiv(zz,8),fx=Math.floorMod(xx,8),fy=Math.floorMod(yy,8),fz=Math.floorMod(zz,8);
        for(int x=gx-(fx<2?1:0);x<=gx+(fx>5?1:0);x++)for(int by=gy-(fy<2?1:0);by<=gy+(fy>5?1:0);by++)for(int z=gz-(fz<2?1:0);z<=gz+(fz>5?1:0);z++){
            var value=accepted(proposal(x,by,z),wx,y,wz,depth,bit,host,caves);if(value!=null)return value;
        }
        return host;
    }
    private BlockState accepted(Proposal p,int wx,int y,int wz,int depth,int bit,BlockState host,boolean caves){
        var k=p.kind;if(k==Kind.EMERALD&&(emeraldMask&bit)==0)return null;
        double chance=k.activation*k.density(depth);if(k==Kind.GOLD&&(goldMask&bit)!=0&&depth>16&&depth<96)chance+=.85*(depth<24?(depth-16)/8.0:depth<=56?1:(96-depth)/40.0);
        if(p.draw>=Math.min(.95,chance))return null;
        double dx=wx+.5-p.x,dy=y+.5-p.y,dz=wz+.5-p.z;if(dx*dx*p.invX+dy*dy*p.invY+dz*dz*p.invZ>=1)return null;
        double discard=k==Kind.DIAMOND?.7:k==Kind.GOLD?((goldMask&bit)!=0&&depth<96?0:.5):k==Kind.LAPIS?.5:0;
        if(caves&&discard>0){long block=mix(seed^(long)wx*0x9E3779B97F4A7C15L^(long)y*0xD6E8FEB86659FD93L^(long)wz*0xA24BAED4963EE407L^0x627572696564L);if(unit(block)<discard&&(naturalVoid(wx+1,y,wz)||naturalVoid(wx-1,y,wz)||naturalVoid(wx,y+1,wz)||naturalVoid(wx,y-1,wz)||naturalVoid(wx,y,wz+1)||naturalVoid(wx,y,wz-1)))return null;}
        return host.is(Blocks.DEEPSLATE)?k.deepslate:k.stone;
    }
    private boolean naturalVoid(int x,int y,int z){return caveField.voidAt(x,y,z,TerrainShape.CEILING);}
    public boolean maybeHas(Predicate<BlockState> p){if(!enabled())return false;for(var k:KINDS)if(p.test(k.stone)||p.test(k.deepslate))return true;return definition>=3&&(p.test(blockbranch.metallurgy.MetallurgyContent.TIN_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.MetallurgyContent.DEEPSLATE_TIN_ORE.defaultBlockState()))||definition>=4&&(p.test(blockbranch.metallurgy.ZincBrassContent.ZINC_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.ZincBrassContent.DEEPSLATE_ZINC_ORE.defaultBlockState()))||definition>=5&&(p.test(blockbranch.metallurgy.SteelContent.MANGANESE_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.SteelContent.DEEPSLATE_MANGANESE_ORE.defaultBlockState()))||definition>=6&&(p.test(blockbranch.metallurgy.TungstenContent.TUNGSTEN_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.TungstenContent.DEEPSLATE_TUNGSTEN_ORE.defaultBlockState()))||definition>=7&&(p.test(blockbranch.metallurgy.StainlessContent.NICKEL_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.StainlessContent.DEEPSLATE_NICKEL_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.StainlessContent.CHROMIUM_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.StainlessContent.DEEPSLATE_CHROMIUM_ORE.defaultBlockState()))||definition>=8&&(p.test(blockbranch.metallurgy.LeadContent.LEAD_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.LeadContent.DEEPSLATE_LEAD_ORE.defaultBlockState()))||definition>=9&&(p.test(blockbranch.metallurgy.FoolsDiamondContent.ORE.defaultBlockState())||p.test(blockbranch.metallurgy.FoolsDiamondContent.DEEPSLATE_ORE.defaultBlockState()))||definition>=10&&(p.test(blockbranch.metallurgy.PreciousContent.SILVER_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.PreciousContent.DEEPSLATE_SILVER_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.PreciousContent.PLATINUM_ORE.defaultBlockState())||p.test(blockbranch.metallurgy.PreciousContent.DEEPSLATE_PLATINUM_ORE.defaultBlockState()))||definition>=11&&(p.test(blockbranch.metallurgy.TitaniumContent.ORE.defaultBlockState())||p.test(blockbranch.metallurgy.TitaniumContent.DEEPSLATE_ORE.defaultBlockState()))||definition>=12&&(p.test(blockbranch.metallurgy.MagnetiteContent.ORE.defaultBlockState())||p.test(blockbranch.metallurgy.MagnetiteContent.DEEPSLATE_ORE.defaultBlockState()));}
    public boolean affectsPredicate(Predicate<BlockState> p){if(!enabled())return false;boolean stone=p.test(Blocks.STONE.defaultBlockState()),deep=p.test(Blocks.DEEPSLATE.defaultBlockState());for(var k:KINDS)if(p.test(k.stone)!=stone||p.test(k.deepslate)!=deep)return true;return definition>=3&&(p.test(blockbranch.metallurgy.MetallurgyContent.TIN_ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.MetallurgyContent.DEEPSLATE_TIN_ORE.defaultBlockState())!=deep)||definition>=4&&(p.test(blockbranch.metallurgy.ZincBrassContent.ZINC_ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.ZincBrassContent.DEEPSLATE_ZINC_ORE.defaultBlockState())!=deep)||definition>=5&&(p.test(blockbranch.metallurgy.SteelContent.MANGANESE_ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.SteelContent.DEEPSLATE_MANGANESE_ORE.defaultBlockState())!=deep)||definition>=6&&(p.test(blockbranch.metallurgy.TungstenContent.TUNGSTEN_ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.TungstenContent.DEEPSLATE_TUNGSTEN_ORE.defaultBlockState())!=deep)||definition>=7&&(p.test(blockbranch.metallurgy.StainlessContent.NICKEL_ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.StainlessContent.DEEPSLATE_NICKEL_ORE.defaultBlockState())!=deep||p.test(blockbranch.metallurgy.StainlessContent.CHROMIUM_ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.StainlessContent.DEEPSLATE_CHROMIUM_ORE.defaultBlockState())!=deep)||definition>=8&&(p.test(blockbranch.metallurgy.LeadContent.LEAD_ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.LeadContent.DEEPSLATE_LEAD_ORE.defaultBlockState())!=deep)||definition>=9&&(p.test(blockbranch.metallurgy.FoolsDiamondContent.ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.FoolsDiamondContent.DEEPSLATE_ORE.defaultBlockState())!=deep)||definition>=10&&(p.test(blockbranch.metallurgy.PreciousContent.SILVER_ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.PreciousContent.DEEPSLATE_SILVER_ORE.defaultBlockState())!=deep||p.test(blockbranch.metallurgy.PreciousContent.PLATINUM_ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.PreciousContent.DEEPSLATE_PLATINUM_ORE.defaultBlockState())!=deep)||definition>=11&&(p.test(blockbranch.metallurgy.TitaniumContent.ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.TitaniumContent.DEEPSLATE_ORE.defaultBlockState())!=deep)||definition>=12&&(p.test(blockbranch.metallurgy.MagnetiteContent.ORE.defaultBlockState())!=stone||p.test(blockbranch.metallurgy.MagnetiteContent.DEEPSLATE_ORE.defaultBlockState())!=deep);}
    public CompoundTag encode(){if(!enabled())throw new IllegalStateException("No ore descriptor");var tag=new CompoundTag();tag.putInt("definition",definition);tag.putLong("seed",seed);tag.putInt("column_x",cx);tag.putInt("column_z",cz);tag.putInt("emerald_mask",emeraldMask);tag.putInt("gold_mask",goldMask);return tag;}
    public static OreLayer decode(CompoundTag tag){return new OreLayer(tag.getLong("seed").orElseThrow(),tag.getInt("column_x").orElseThrow(),tag.getInt("column_z").orElseThrow(),tag.getInt("definition").orElseThrow(),tag.getInt("emerald_mask").orElseThrow(),tag.getInt("gold_mask").orElseThrow());}
    private static double unit(long h){return (h&65535)/65536.0;}
    private static long mix(long h){h=(h^(h>>>30))*0xBF58476D1CE4E5B9L;h=(h^(h>>>27))*0x94D049BB133111EBL;return h^(h>>>31);}
}
