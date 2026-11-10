package blockbranch.client.horizon;
import static blockbranch.horizon.HorizonTiles.*;
import java.nio.*;
import java.util.function.IntFunction;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
/**
 * Builds one tile's quads on the mesher thread: a top per cell, risers where a neighbour
 * is lower, and a translucent water surface. Vertices are relative to the tile origin
 * (the shader adds it as the chunk-section position) and carry tiling coordinates in
 * blocks, so a merged or coarse face repeats the native 16-pixel texture once per block.
 *
 * <p>Lighting follows vanilla smooth lighting: ambient occlusion darkens the vertices of
 * a face beside full blocks, and light under a crown is averaged per vertex.
 *
 * <p>Tile edges always get a skirt down to below the lowest ground in the tile, hiding the
 * cracks between neighbouring tiles of different levels without needing their data.
 */
final class HorizonMesher {
    static final int STRIDE=44;
    /** Vertex-alpha flags read by the fragment shader; prefixed so they never shadow tile fields such as {@code WATER}. */
    static final int FLAG_OPAQUE=255,FLAG_LAYERED=192,FLAG_CUTOUT=128,FLAG_WATER=64;
    private static final float WATER_SURFACE=8/9f;
    record Mesh(long key,ByteBuffer vertices,int solidQuads,int waterQuads) {}

    private final HorizonMaterials materials;
    private final IntFunction<Biome> biomes;
    private ByteBuffer out=ByteBuffer.allocate(STRIDE*4*1024).order(ByteOrder.nativeOrder());
    private int quads;

    HorizonMesher(HorizonMaterials materials,IntFunction<Biome> biomes){this.materials=materials;this.biomes=biomes;}

    Mesh build(long key,int[] d) {
        out.clear();quads=0;
        int level=level(key),c=cell(level),ox=originX(key),oz=originZ(key);
        float tileMin=Float.MAX_VALUE;for(int j=1;j<=CELLS;j++)for(int i=1;i<=CELLS;i++)tileMin=Math.min(tileMin,groundSurface(d,(j*SAMPLES+i)*FIELDS));
        var light=CardinalLighting.DEFAULT;
        float[] shaded=canopyLight(d,c);
        for(int j=1;j<=CELLS;j++) {
            // Tops merge along x while every vertex attribute matches.
            int runStart=-1;float runH=0;HorizonMaterials.Face runFace=null;int runColor=0,runSky=0,runFlag=0;
            for(int i=1;i<=CELLS+1;i++) {
                float h=0;HorizonMaterials.Face face=null;int color=0,sky=0,flag=0;boolean occluded=false;
                if(i<=CELLS) {
                    int s=(j*SAMPLES+i)*FIELDS;var m=materials.get(d[s+TOP_STATE]);
                    h=surface(d,s);face=m.top();color=blend(d,i,j,m,face,ox,oz,level);sky=sky(d,s,d[s+TOP]);flag=flag(face);
                    int above=d[s+TOP]+(m.height()>=1?1:0);
                    for(int v=0;v<4;v++){ao[v]=topOcclusion(d,i,j,CORNERS[v][0],CORNERS[v][1],above,c);occluded|=ao[v]<1;}
                }
                if(runStart>=0&&(i>CELLS||occluded||h!=runH||face!=runFace||color!=runColor||sky!=runSky||flag!=runFlag)) {
                    float x0=(runStart-1)*c,x1=(i-1)*c,z0=(j-1)*c,z1=j*c;
                    quad(x0,runH,z0, x0,runH,z1, x1,runH,z1, x1,runH,z0, x0,z0, x0,z1, x1,z1, x1,z0, shade(runColor,light.up()),runFlag,runSky,runFace);
                    runStart=-1;
                }
                if(i<=CELLS&&occluded) {
                    // An occluded top keeps its own vertices, so it never joins a run.
                    float x0=(i-1)*c,x1=i*c,z0=(j-1)*c,z1=j*c;
                    lit(x0,h,z0, x0,h,z1, x1,h,z1, x1,h,z0, x0,z0, x0,z1, x1,z1, x1,z0, shade(color,light.up()),ao[0],ao[1],ao[2],ao[3],sky,sky,sky,sky,flag,face);
                } else if(i<=CELLS&&runStart<0){runStart=i;runH=h;runFace=face;runColor=color;runSky=sky;runFlag=flag;}
            }
            for(int i=1;i<=CELLS;i++){risers(d,i,j,c,ox,oz,level,tileMin,light);underCanopy(d,i,j,c,ox,oz,level,light,shaded);}
        }
        int solid=quads;
        var water=materials.water();var waterState=Blocks.WATER.defaultBlockState();
        for(int j=1;j<=CELLS;j++)for(int i=1;i<=CELLS;i++) {
            int s=(j*SAMPLES+i)*FIELDS;if(d[s+WATER]==NONE)continue;
            float h=d[s+WATER]+WATER_SURFACE,x0=(i-1)*c,x1=i*c,z0=(j-1)*c,z1=j*c;
            int color=0;int r=0,g=0,b=0,n=0;
            for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){int t=(( (j+dz)*SAMPLES+i+dx)*FIELDS);int col=materials.tint(waterState,water,biomes.apply(d[t+BIOME]),sampleCoordinate(ox,level,i+dx),sampleCoordinate(oz,level,j+dz));r+=col>>16&255;g+=col>>8&255;b+=col&255;n++;}
            color=(r/n)<<16|(g/n)<<8|(b/n);
            quad(x0,h,z0, x0,h,z1, x1,h,z1, x1,h,z0, x0,z0, x0,z1, x1,z1, x1,z0, color,FLAG_WATER,15,water);
        }
        var vertices=ByteBuffer.allocateDirect(out.position()).order(ByteOrder.nativeOrder());out.flip();vertices.put(out).flip();
        return new Mesh(key,vertices,solid,quads-solid);
    }

    /** Cutout faces (leaves, azalea, bushes) are drawn opaque with deeper layers in their gaps. */
    private static int flag(HorizonMaterials.Face face){return face.cutout()?FLAG_LAYERED:FLAG_OPAQUE;}
    private static final int[][] DIRS={{0,-1},{0,1},{-1,0},{1,0}};// north, south, west, east
    /** Top-face vertex order: (x0,z0), (x0,z1), (x1,z1), (x1,z0). */
    private static final int[][] CORNERS={{-1,-1},{-1,1},{1,1},{1,-1}};
    private final float[] ao=new float[4];
    /** Vanilla's shade brightness: 0.2 at a full block, 1 elsewhere. */
    private static float shadeAt(boolean solid){return solid?.2f:1;}
    /** Whether sample {@code o}'s column holds a full block at layer {@code y}. */
    private boolean solid(int[] d,int o,int y) {
        if(floating(d,o)&&y>=d[o+CANOPY_BOTTOM])return y<=d[o+TOP];
        boolean floats=floating(d,o);int top=floats?d[o+GROUND]:d[o+TOP];
        return y<top||y==top&&materials.get(d[o+(floats?GROUND_STATE:TOP_STATE)]).height()>=1;
    }
    /**
     * Ambient occlusion at one top-face vertex, as vanilla computes it from the two side
     * blocks and the corner block in the layer above the face, with the corner counted as
     * blocked when both sides are. A coarse cell spreads vanilla's one-block dark strip
     * over its whole width, so the darkening is divided by the cell size to keep the
     * same average.
     */
    private float topOcclusion(int[] d,int i,int j,int sx,int sz,int y,int c) {
        boolean a=solid(d,((j)*SAMPLES+i+sx)*FIELDS,y),b=solid(d,((j+sz)*SAMPLES+i)*FIELDS,y);
        boolean k=a&&b||solid(d,((j+sz)*SAMPLES+i+sx)*FIELDS,y);
        float v=(shadeAt(a)+shadeAt(b)+shadeAt(k)+1)/4;
        return 1-(1-v)/c;
    }
    /**
     * Sky light in the air under each floating cell, in 0..15. Vanilla light spreads in
     * sideways from open ground, losing one per block, or comes down through the leaves
     * and the air gap. Open cells read 15; trunks and bushes are neither open nor floating
     * and only pass light on. Samples beyond the tile border count as open. Null when no
     * cell floats.
     */
    private float[] canopyLight(int[] d,int c) {
        int n=SAMPLES*SAMPLES,far=1<<20;int[] k=new int[n];boolean any=false;
        for(int t=0;t<n;t++){int o=t*FIELDS;boolean f=floating(d,o);any|=f;k[t]=f||materials.get(d[o+TOP_STATE]).top().cutout()?far:0;}
        if(!any)return null;
        for(int j=0;j<SAMPLES;j++)for(int i=0;i<SAMPLES;i++){int t=j*SAMPLES+i;if(k[t]!=0)k[t]=Math.min(k[t],Math.min(i>0?k[t-1]:0,j>0?k[t-SAMPLES]:0)+1);}
        for(int j=SAMPLES-1;j>=0;j--)for(int i=SAMPLES-1;i>=0;i--){int t=j*SAMPLES+i;if(k[t]!=0)k[t]=Math.min(k[t],Math.min(i<SAMPLES-1?k[t+1]:0,j<SAMPLES-1?k[t+SAMPLES]:0)+1);}
        float[] l=new float[n];
        for(int t=0;t<n;t++) {
            if(k[t]==0){l[t]=15;continue;}
            int o=t*FIELDS;float side=15-(k[t]*c-(c-1)/2f),above=15-(d[o+TOP]-d[o+GROUND]-1);
            l[t]=Math.max(0,Math.max(side,above));
        }
        return l;
    }
    /** Smooth light at a vertex: the mean of the four cells around it. */
    private static float vertexLight(float[] l,int i,int j,int sx,int sz){return (l[j*SAMPLES+i]+l[j*SAMPLES+i+sx]+l[(j+sz)*SAMPLES+i]+l[(j+sz)*SAMPLES+i+sx])/4;}
    /** Top surface of a sample: the top of its highest visible block. */
    private float surface(int[] d,int o){return d[o+TOP]+materials.get(d[o+TOP_STATE]).height();}
    /** Top of the solid ground; below a floating canopy this is under the air gap. */
    private float groundSurface(int[] d,int o){return floating(d,o)?d[o+GROUND]+materials.get(d[o+GROUND_STATE]).height():surface(d,o);}
    /**
     * Side faces where this column is exposed. A column is solid up to its ground surface
     * and, under a floating canopy, also from the canopy bottom to its top; each material
     * band is clipped by the same two intervals of the neighbour. Tile edges compare only
     * against a skirt below the tile's lowest ground, since neighbour tiles may be coarser.
     */
    private void risers(int[] d,int i,int j,int c,int ox,int oz,int level,float tileMin,CardinalLighting light) {
        int s=(j*SAMPLES+i)*FIELDS;var top=materials.get(d[s+TOP_STATE]);var ground=materials.get(d[s+GROUND_STATE]);var deep=materials.get(d[s+DEEP_STATE]);
        boolean floats=floating(d,s);int g=d[s+GROUND];float h=surface(d,s),gh=g+ground.height(),cb=d[s+CANOPY_BOTTOM];
        int topFlag=flag(top.side());
        for(int dir=0;dir<4;dir++) {
            int ni=i+DIRS[dir][0],nj=j+DIRS[dir][1],n=(nj*SAMPLES+ni)*FIELDS;
            boolean edge=ni==0||nj==0||ni==SAMPLES-1||nj==SAMPLES-1;
            float floor=groundSurface(d,n),low=edge?Math.min(floor,tileMin)-c:floor;
            float canopyLow=Float.NaN,canopyHigh=Float.NaN;
            if(!edge&&floating(d,n)){canopyLow=d[n+CANOPY_BOTTOM];canopyHigh=surface(d,n);}
            float shade=dir<2?light.north():light.west();
            if(floats) {
                exposed(dir,i,j,c,cb,h,low,floor,canopyLow,canopyHigh,top,top.side(),d,ox,oz,level,shade,topFlag);
                exposed(dir,i,j,c,g,gh,low,floor,canopyLow,canopyHigh,ground,ground.side(),d,ox,oz,level,shade,flag(ground.side()));
                if(ground.overlay()!=null)exposed(dir,i,j,c,g,gh,low,floor,canopyLow,canopyHigh,ground,ground.overlay(),d,ox,oz,level,shade,FLAG_CUTOUT);
                exposed(dir,i,j,c,low,g,low,floor,canopyLow,canopyHigh,deep,deep.side(),d,ox,oz,level,shade,flag(deep.side()));
            } else {
                if(d[s+TOP]>g)exposed(dir,i,j,c,gh,h,low,floor,canopyLow,canopyHigh,top,top.side(),d,ox,oz,level,shade,topFlag);
                exposed(dir,i,j,c,g,Math.min(h,gh),low,floor,canopyLow,canopyHigh,ground,ground.side(),d,ox,oz,level,shade,flag(ground.side()));
                if(ground.overlay()!=null)exposed(dir,i,j,c,g,Math.min(h,gh),low,floor,canopyLow,canopyHigh,ground,ground.overlay(),d,ox,oz,level,shade,FLAG_CUTOUT);
                exposed(dir,i,j,c,low,Math.min(h,g),low,floor,canopyLow,canopyHigh,deep,deep.side(),d,ox,oz,level,shade,flag(deep.side()));
            }
        }
    }
    /** Emits the parts of [y0,y1] above the neighbour's ground and outside its canopy; {@code floor} is the neighbour's ground surface. */
    private void exposed(int dir,int i,int j,int c,float y0,float y1,float low,float floor,float canopyLow,float canopyHigh,HorizonMaterials.Material m,HorizonMaterials.Face face,int[] d,int ox,int oz,int level,float shade,int flag) {
        y0=Math.max(y0,low);
        if(y1<=y0)return;
        int s=(j*SAMPLES+i)*FIELDS;
        if(Float.isNaN(canopyLow)||canopyHigh<=y0||canopyLow>=y1){band(dir,i,j,c,y0,y1,floor,m,face,d,ox,oz,level,shade,sky(d,s,(int)Math.floor(y0)),flag);return;}
        if(canopyLow>y0)band(dir,i,j,c,y0,canopyLow,floor,m,face,d,ox,oz,level,shade,sky(d,s,(int)Math.floor(y0)),flag);
        if(canopyHigh<y1)band(dir,i,j,c,canopyHigh,y1,canopyHigh,m,face,d,ox,oz,level,shade,sky(d,s,(int)Math.floor(canopyHigh)),flag);
    }
    /** Underside of a floating canopy and the ground top beneath it, which the top pass does not draw. */
    private void underCanopy(int[] d,int i,int j,int c,int ox,int oz,int level,CardinalLighting light,float[] l) {
        int s=(j*SAMPLES+i)*FIELDS;if(!floating(d,s))return;
        var top=materials.get(d[s+TOP_STATE]);var ground=materials.get(d[s+GROUND_STATE]);
        float x0=(i-1)*c,x1=i*c,z0=(j-1)*c,z1=j*c,y=d[s+CANOPY_BOTTOM],gh=d[s+GROUND]+ground.height();
        float l00=vertexLight(l,i,j,-1,-1),l01=vertexLight(l,i,j,-1,1),l11=vertexLight(l,i,j,1,1),l10=vertexLight(l,i,j,1,-1);
        lit(x1,y,z0, x1,y,z1, x0,y,z1, x0,y,z0, x1,z0, x1,z1, x0,z1, x0,z0, shade(blend(d,i,j,top,top.top(),ox,oz,level),light.down()),1,1,1,1,l10,l11,l01,l00,flag(top.top()),top.top());
        lit(x0,gh,z0, x0,gh,z1, x1,gh,z1, x1,gh,z0, x0,z0, x0,z1, x1,z1, x1,z0, shade(blend(d,i,j,ground,ground.top(),ox,oz,level),light.up()),1,1,1,1,l00,l01,l11,l10,flag(ground.top()),ground.top());
    }
    /**
     * One side quad from y0 to y1. Where the neighbour's ground or canopy top ({@code floor})
     * meets the wall, its lowest block gets vanilla's ambient occlusion: the bottom vertices
     * see the solid floor below them, and the floor or wall beside them.
     */
    private void band(int dir,int i,int j,int c,float y0,float y1,float floor,HorizonMaterials.Material m,HorizonMaterials.Face face,int[] d,int ox,int oz,int level,float shade,int sky,int flag) {
        if(y1<=y0)return;
        float aMinus=1,aPlus=1,top=y1;
        if(floor>=y0&&floor<y1) {
            if(floor>y0)band(dir,i,j,c,y0,floor,Float.NaN,m,face,d,ox,oz,level,shade,sky,flag);
            y0=floor;top=Math.min(floor+1,y1);
            int ni=i+DIRS[dir][0],nj=j+DIRS[dir][1],pi=dir<2?1:0,pj=dir<2?0:1,y=(int)Math.ceil(floor-1e-3f);
            aMinus=sideOcclusion(d,((nj-pj)*SAMPLES+ni-pi)*FIELDS,y);aPlus=sideOcclusion(d,((nj+pj)*SAMPLES+ni+pi)*FIELDS,y);
        }
        int color=shade(blend(d,i,j,m,face,ox,oz,level),shade);
        float x0=(i-1)*c,x1=i*c,z0=(j-1)*c,z1=j*c,b=y0,t=top;
        switch(dir) {
            case 0->lit(x1,t,z0, x1,b,z0, x0,b,z0, x0,t,z0, -x1,-t, -x1,-b, -x0,-b, -x0,-t, color,1,aPlus,aMinus,1,sky,sky,sky,sky,flag,face);
            case 1->lit(x0,t,z1, x0,b,z1, x1,b,z1, x1,t,z1, x0,-t, x0,-b, x1,-b, x1,-t, color,1,aMinus,aPlus,1,sky,sky,sky,sky,flag,face);
            case 2->lit(x0,t,z0, x0,b,z0, x0,b,z1, x0,t,z1, z0,-t, z0,-b, z1,-b, z1,-t, color,1,aMinus,aPlus,1,sky,sky,sky,sky,flag,face);
            default->lit(x1,t,z1, x1,b,z1, x1,b,z0, x1,t,z0, -z1,-t, -z1,-b, -z0,-b, -z0,-t, color,1,aPlus,aMinus,1,sky,sky,sky,sky,flag,face);
        }
        if(top<y1)band(dir,i,j,c,top,y1,Float.NaN,m,face,d,ox,oz,level,shade,sky,flag);
    }
    /** Occlusion at a bottom side vertex: the floor below is solid; {@code o} is the sample beside it. */
    private float sideOcclusion(int[] d,int o,int y) {
        boolean beside=solid(d,o,y),corner=beside||solid(d,o,y-1);
        return (shadeAt(true)+shadeAt(beside)+shadeAt(corner)+1)/4;
    }
    /** Sky light at a face: full in air, reduced by one per water block above it, as in vanilla water. */
    private static int sky(int[] d,int s,int y){int w=d[s+WATER];return w==NONE||w<y?15:Math.max(0,15-(w-y));}
    /** Tint averaged over the 3×3 neighbouring samples' biomes, like vanilla biome blending. */
    private int blend(int[] d,int i,int j,HorizonMaterials.Material m,HorizonMaterials.Face face,int ox,int oz,int level) {
        if(face.tintIndex()<0)return 0xFFFFFF;
        int r=0,g=0,b=0;
        for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++) {
            int t=((j+dz)*SAMPLES+i+dx)*FIELDS;
            int col=materials.tint(m.state(),face,biomes.apply(d[t+BIOME]),sampleCoordinate(ox,level,i+dx),sampleCoordinate(oz,level,j+dz));
            r+=col>>16&255;g+=col>>8&255;b+=col&255;
        }
        return (r/9)<<16|(g/9)<<8|(b/9);
    }
    private static int shade(int rgb,float f){return Math.round((rgb>>16&255)*f)<<16|Math.round((rgb>>8&255)*f)<<8|Math.round((rgb&255)*f);}

    private void quad(float x0,float y0,float z0,float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,
                      float u0,float v0,float u1,float v1,float u2,float v2,float u3,float v3,int rgb,int flag,int sky,HorizonMaterials.Face f) {
        lit(x0,y0,z0,x1,y1,z1,x2,y2,z2,x3,y3,z3,u0,v0,u1,v1,u2,v2,u3,v3,rgb,1,1,1,1,sky,sky,sky,sky,flag,f);
    }
    /** A quad with per-vertex occlusion {@code a0..a3} (colour factor) and sky light {@code s0..s3} (0..15, fractional for smooth light). */
    private void lit(float x0,float y0,float z0,float x1,float y1,float z1,float x2,float y2,float z2,float x3,float y3,float z3,
                     float u0,float v0,float u1,float v1,float u2,float v2,float u3,float v3,int rgb,float a0,float a1,float a2,float a3,
                     float s0,float s1,float s2,float s3,int flag,HorizonMaterials.Face f) {
        if(out.remaining()<STRIDE*4){var grown=ByteBuffer.allocate(out.capacity()*2).order(ByteOrder.nativeOrder());out.flip();grown.put(out);out=grown;}
        vertex(x0,y0,z0,u0,v0,shade(rgb,a0),flag,s0,f);vertex(x1,y1,z1,u1,v1,shade(rgb,a1),flag,s1,f);
        vertex(x2,y2,z2,u2,v2,shade(rgb,a2),flag,s2,f);vertex(x3,y3,z3,u3,v3,shade(rgb,a3),flag,s3,f);
        quads++;
    }
    private void vertex(float x,float y,float z,float u,float v,int rgb,int flag,float sky,HorizonMaterials.Face f) {
        out.putFloat(x).putFloat(y).putFloat(z);
        out.put((byte)(rgb>>16)).put((byte)(rgb>>8)).put((byte)rgb).put((byte)flag);
        out.putFloat(u).putFloat(v);
        out.putShort((short)0).putShort((short)Math.round(sky*16));
        out.putFloat(f.u0()).putFloat(f.v0()).putFloat(f.u1()).putFloat(f.v1());
    }
}
