package blockbranch.realistic;

import java.util.Arrays;

/**
 * Immutable drainage over the finite packed world. A 4 km mesh is solved once
 * per generator/seed; block sampling only inspects nearby edges. Priority-flood
 * routing preserves selected inland sinks instead of breaching every depression.
 * No precipitation, evaporation, erosion or biome simulation is performed.
 */
public final class DrainageNetwork {
    public static final int STEP = 4096, SIDE = 1025, MIN = -2097152;
    public static final int DRY = Integer.MIN_VALUE;
    private static final int COUNT = SIDE * SIDE, RIVER_AREA = 8;
    private final long seed;
    private final TerrainShape shape;
    private final boolean coastalLandforms;
    private final boolean sandyCoasts;
    private final SandyCoast sandyCoast;
    private final boolean erodedLandforms;
    private volatile DrainageNetwork preformation;
    private volatile ErodedBadlands eroded;
    private final boolean gravelReliefEnabled;
    private final boolean surfaceOwnership;
    private final DeepBasinRelief deepBasinRelief;
    private final boolean landIceEnabled,continentalIceEnabled;
    private volatile ContinentalIceField continentalIce;
    private volatile LandIceField landIce;
    private volatile DrainageNetwork iceReference;
    private volatile DrainageNetwork unrelieved;
    private volatile GravelDesertRelief gravelRelief;
    private final CoastalHeadlands headlands;
    // Disposable client comparison only; never enabled by normal generation.
    private final boolean structuralComparison=Boolean.getBoolean("blockbranch.terrainStructuralComparison")&&Boolean.getBoolean("blockbranch.realisticClientFixture");
    private final TerrainRelief surfaceRelief=new TerrainRelief(this::structuralSample);
    // Exact block samples are immutable. A bounded direct-mapped cache avoids
    // recalculating shared climate/slope probes without spatial approximation.
    private final SurfaceReservationCache surfaceReservations = new SurfaceReservationCache((x,z)->LandVegetation.uncachedSurfaceReserved(this,x,z));
    boolean surfaceReserved(int x,int z){return surfaceReservations.reserved(x,z);}
    // Fixed per-network budget: 131072 exact samples; the preformation view
    // has an independent budget. Entries retain only immutable scalar samples.
    // Lock-free: a slot holds one immutable entry, so a racy read sees null, an older
    // entry or a newer one, never a key paired with another column's sample (final-field
    // publication). A monitor here was contended by every generation worker.
    private record CachedSample(long key, Sample value) {}
    private final CachedSample[] sampleCache = new CachedSample[1 << 17];
    private final short[] ground;
    private final int[] parent, root, area, water;
    private final boolean[] ocean;
    private final float[] tangentX, tangentZ;
    // Node positions are pure functions of (seed, index) but cost a hash and a division each,
    // and curve evaluation asks for them thousands of times per column. Shared with views.
    private final int[] nodeXs, nodeZs;
    // River edge i keeps its Bezier control points {c.x, c.z, e.x, e.z} at edgeControl[4*edgeSlot[i]],
    // fixed once the graph and tangents are final. Non-river edges have slot -1 and compute directly.
    private final int[] edgeSlot;
    private final double[] edgeControl;

    /** Water level is the first air Y, matching the sea-level convention. */
    public record Sample(int height, int waterLevel, int basin, boolean closed,boolean bedrockCoast,int regionalHeight,int regionalWaterLevel,boolean sandyBackshore) {
        public Sample(int height,int water,int basin,boolean closed,boolean rock,int regionalHeight,int regionalWater){this(height,water,basin,closed,rock,regionalHeight,regionalWater,true);}
        public Sample(int height,int waterLevel,int basin,boolean closed){this(height,waterLevel,basin,closed,false,height,waterLevel);}
    }

    public DrainageNetwork(TerrainShape shape,long seed){this(shape,seed,true);}
    /** Read-only negative control for coastal regression; ordinary worlds always enable the foundation. */
    DrainageNetwork(TerrainShape shape,long seed,boolean coastalLandforms) {this(shape,seed,coastalLandforms,false);}
    DrainageNetwork(TerrainShape shape,long seed,boolean coastalLandforms,boolean sandyCoasts) {
        this(shape,seed,coastalLandforms,sandyCoasts,false);
    }
    DrainageNetwork(TerrainShape shape,long seed,boolean coastalLandforms,boolean sandyCoasts,boolean gravelReliefEnabled) {
        this(shape,seed,coastalLandforms,sandyCoasts,gravelReliefEnabled,false);
    }
    DrainageNetwork(TerrainShape shape,long seed,boolean coastalLandforms,boolean sandyCoasts,boolean gravelReliefEnabled,boolean surfaceOwnership) {
        this(shape,seed,coastalLandforms,sandyCoasts,gravelReliefEnabled,surfaceOwnership,false);
    }
    DrainageNetwork(TerrainShape shape,long seed,boolean coastalLandforms,boolean sandyCoasts,boolean gravelReliefEnabled,boolean surfaceOwnership,boolean deepBasinRelief) {
        this(shape,seed,coastalLandforms,sandyCoasts,gravelReliefEnabled,surfaceOwnership,deepBasinRelief,false);
    }
    DrainageNetwork(TerrainShape shape,long seed,boolean coastalLandforms,boolean sandyCoasts,boolean gravelReliefEnabled,boolean surfaceOwnership,boolean deepBasinRelief,boolean landIceEnabled) {
        this(shape,seed,coastalLandforms,sandyCoasts,gravelReliefEnabled,surfaceOwnership,deepBasinRelief,landIceEnabled,false);
    }
    DrainageNetwork(TerrainShape shape,long seed,boolean coastalLandforms,boolean sandyCoasts,boolean gravelReliefEnabled,boolean surfaceOwnership,boolean deepBasinRelief,boolean landIceEnabled,boolean continentalIceEnabled) {
        this.continentalIceEnabled=continentalIceEnabled;
        this.landIceEnabled=landIceEnabled;
        this.deepBasinRelief=deepBasinRelief?new DeepBasinRelief(shape):null;
        this.surfaceOwnership=surfaceOwnership;
        this.gravelReliefEnabled=gravelReliefEnabled;
        this.sandyCoasts=sandyCoasts;this.sandyCoast=new SandyCoast(seed,shape,this);
        this.coastalLandforms=coastalLandforms;
        this.erodedLandforms=true;
        ground=new short[COUNT];parent=new int[COUNT];root=new int[COUNT];area=new int[COUNT];water=new int[COUNT];ocean=new boolean[COUNT];tangentX=new float[COUNT];tangentZ=new float[COUNT];
        this.headlands=new CoastalHeadlands(seed,shape);
        this.shape = shape;
        this.seed = seed;
        nodeXs=new int[COUNT];nodeZs=new int[COUNT];
        java.util.stream.IntStream.range(0, COUNT).parallel().forEach(i -> {nodeXs[i]=computeNodeX(i);nodeZs[i]=computeNodeZ(i);});
        Arrays.fill(parent, -2);
        Arrays.fill(area, 1);
        // Each node's height is a pure function of (seed, x, z) and writes only its own slot, so
        // the ~1M evaluations are spread over the common pool with results identical to a serial loop.
        java.util.stream.IntStream.range(0, COUNT).parallel().forEach(i -> ground[i] = (short)shape.height(nodeX(i), nodeZ(i)));
        int[] order = new int[COUNT];
        int head = 0, tail = 0;
        // Only below-sea terrain connected to a world edge is an ocean seed.
        // Enclosed below-sea depressions are not automatically ocean outlets.
        for (int z = 0; z < SIDE; z++) for (int x = 0; x < SIDE; x++)
            if (x == 0 || z == 0 || x == SIDE - 1 || z == SIDE - 1) {
                int i = z * SIDE + x;
                if (ground[i] < TerrainShape.SEA_LEVEL && !ocean[i]) { ocean[i] = true; order[tail++] = i; }
            }
        while (head < tail) {
            int i = order[head++], x = i % SIDE, z = i / SIDE;
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                if (!neighbor(x, z, dx, dz)) continue;
                int n = i + dz * SIDE + dx;
                if (!ocean[n] && ground[n] < TerrainShape.SEA_LEVEL) { ocean[n] = true; order[tail++] = n; }
            }
        }
        if (tail == 0) throw new IllegalStateException("Drainage domain has no ocean outlet");
        int[] flood = new int[COUNT];
        var heap = new Heap(flood);
        for (int j = 0; j < tail; j++) {
            int i = order[j];
            parent[i] = -1; root[i] = i; water[i] = flood[i] = TerrainShape.SEA_LEVEL;
            int x = i % SIDE, z = i / SIDE;
            boolean shore = false;
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++)
                if (neighbor(x, z, dx, dz) && !ocean[i + dz * SIDE + dx]) shore = true;
            if (shore) heap.add(i);
        }
        // Natural depressions are retained in broad seeded regions. This is a
        // topology choice, not an aridity/biome model or a fixed basin-count quota.
        for (int z = 1; z < SIDE - 1; z++) for (int x = 1; x < SIDE - 1; x++) {
            int i = z * SIDE + x;
            if (ocean[i] || ground[i] < TerrainShape.SEA_LEVEL + 120 || !shape.closedBasinRegion(nodeX(i), nodeZ(i))) continue;
            boolean minimum = true;
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++)
                if (neighbor(x, z, dx, dz) && ground[i + dz * SIDE + dx] <= ground[i]) minimum = false;
            if (!minimum) continue;
            // Check the actual terrain rim, rather than trusting mesh minima.
            double radius = lakeRadius(i);
            boolean rim = true;
            for (int k = 0; k < 32; k++) {
                double angle = 2 * Math.PI * k / 32;
                double r = radius * shoreScale(i, angle);
                if (shape.height(nodeX(i) + (int)(r * Math.cos(angle)), nodeZ(i) + (int)(r * Math.sin(angle))) < ground[i] - 4) rim = false;
            }
            if (!rim) continue;
            parent[i] = -1; root[i] = i; water[i] = flood[i] = ground[i] - 12;
            heap.add(i);
        }
        while (!heap.empty()) {
            int i = heap.remove();
            if (!ocean[i]) order[tail++] = i;
            int x = i % SIDE, z = i / SIDE;
            for (int dz = -1; dz <= 1; dz++) for (int dx = -1; dx <= 1; dx++) {
                if (!neighbor(x, z, dx, dz)) continue;
                int n = i + dz * SIDE + dx;
                if (parent[n] != -2) continue;
                parent[n] = i; root[n] = root[i];
                water[n] = ground[n] - 4;
                flood[n] = Math.max(ground[n], flood[i]);
                heap.add(n);
            }
        }
        if (tail != COUNT) throw new IllegalStateException("Unrouted drainage cells: " + (COUNT - tail));
        for (int j = COUNT - 1; j >= 0; j--) {
            int i = order[j];
            if (parent[i] >= 0) area[parent[i]] += area[i];
        }
        int[] mainTributary = new int[COUNT];
        Arrays.fill(mainTributary, -1);
        for (int i = 0; i < COUNT; i++) {
            int p = parent[i];
            if (p >= 0 && (mainTributary[p] < 0 || area[i] > area[mainTributary[p]])) mainTributary[p] = i;
        }
        for (int i = 0; i < COUNT; i++) {
            int p = parent[i], child = mainTributary[i];
            double vx = 0, vz = 0;
            if (p >= 0) {
                double length = edgeLength(i);
                vx = (nodeX(p) - nodeX(i)) / length; vz = (nodeZ(p) - nodeZ(i)) / length;
            }
            if (child >= 0) {
                double length = edgeLength(child);
                double ix = (nodeX(i) - nodeX(child)) / length, iz = (nodeZ(i) - nodeZ(child)) / length;
                if (p < 0) { vx = ix; vz = iz; }
                else if (PlanarMath.length(vx + ix, vz + iz) > 0.2) { vx += ix; vz += iz; }
            }
            double length = PlanarMath.length(vx, vz);
            if (length > 0) { tangentX[i] = (float)(vx / length); tangentZ[i] = (float)(vz / length); }
        }
        // Bound channel profiles by the terrain along their curved route. Then
        // propagate low points downstream, carving spill barriers into gorges
        // instead of putting unsupported ribbons of water above valley floors.
        // The ~4M height samples dominate construction. Each limit depends only on the finished
        // graph, so they are computed in parallel; the min() updates are order-independent and
        // stay serial, leaving water[] identical to a single in-order pass.
        edgeSlot=new int[COUNT];
        int rivers=0;
        for (int i = 0; i < COUNT; i++) edgeSlot[i]=river(i)?rivers++:-1;
        edgeControl=new double[4*rivers];
        java.util.stream.IntStream.range(0, COUNT).parallel().filter(this::river).forEach(i -> {
            int k=4*edgeSlot[i];
            edgeControl[k]=computeControl(i,false,false);edgeControl[k+1]=computeControl(i,false,true);
            edgeControl[k+2]=computeControl(i,true,false);edgeControl[k+3]=computeControl(i,true,true);
        });
        int[] limits = new int[COUNT];
        java.util.stream.IntStream.range(0, COUNT).parallel().filter(this::river).forEach(i -> limits[i] = routeClearance(i));
        for (int i = 0; i < COUNT; i++) if (river(i)) {
            int p = parent[i], limit = limits[i];
            water[i] = Math.min(water[i], limit);
            if (!ocean[p]) water[p] = Math.min(water[p], limit);
        }
        for (int j = COUNT - 1; j >= 0; j--) {
            int i = order[j], p = parent[i];
            if (p >= 0 && !ocean[p]) water[p] = Math.min(water[p], water[i]);
        }
        for (int j = 0; j < COUNT; j++) {
            int i = order[j], p = parent[i];
            if (p >= 0) water[i] = Math.max(water[i], water[p]);
        }
    }

    /** Immutable preformation view shares the solved graph, never a modified sample cache. */
    private DrainageNetwork(DrainageNetwork owner) {this(owner,false);}
    private DrainageNetwork(DrainageNetwork owner,boolean erodedLandforms) {this(owner,erodedLandforms,false);}
    private DrainageNetwork(DrainageNetwork owner,boolean erodedLandforms,boolean completeReference) {
        landIceEnabled=false;continentalIceEnabled=false;
        // Legacy reference views disable this final physical adjustment; apply once,
        // after the complete original surface, retaining its climate references.
        deepBasinRelief=completeReference?owner.deepBasinRelief:null;surfaceOwnership=owner.surfaceOwnership;seed=owner.seed;shape=owner.shape;coastalLandforms=owner.coastalLandforms;sandyCoasts=owner.sandyCoasts;sandyCoast=new SandyCoast(seed,shape,this);this.erodedLandforms=erodedLandforms;headlands=owner.headlands;gravelReliefEnabled=completeReference&&owner.gravelReliefEnabled;
        ground=owner.ground;parent=owner.parent;root=owner.root;area=owner.area;water=owner.water;ocean=owner.ocean;
        tangentX=owner.tangentX;tangentZ=owner.tangentZ;
        nodeXs=owner.nodeXs;nodeZs=owner.nodeZs;edgeSlot=owner.edgeSlot;edgeControl=owner.edgeControl;
    }
    /** Complete pre-ice physical foundation; no self-cooling or terrain feedback. */
    DrainageNetwork iceReference(){
        if(!landIceEnabled)return this;var value=iceReference;if(value!=null)return value;
        synchronized(this){if(iceReference==null)iceReference=new DrainageNetwork(this,erodedLandforms,true);return iceReference;}
    }
    private LandIceField landIce(){var value=landIce;if(value!=null)return value;synchronized(this){if(landIce==null)landIce=new LandIceField(seed,iceReference());return landIce;}}
    public int landIceThickness(int x,int z){return landIceEnabled&&!structuralComparison&&continentalIceThickness(x,z)==0?landIce().thickness(x,z):0;}
    private ContinentalIceField continentalIce(){var f=continentalIce;if(f!=null)return f;synchronized(this){if(continentalIce==null)continentalIce=new ContinentalIceField(seed,iceReference());return continentalIce;}}
    public int continentalIceThickness(int x,int z){return continentalIceEnabled&&!structuralComparison&&PolarClimate.latitude(z)>PolarClimate.START?continentalIce().thickness(x,z):0;}
    public boolean permanentFreshwaterLid(int x,int z){return continentalIceEnabled&&!structuralComparison&&PolarClimate.latitude(z)>PolarClimate.START&&continentalIce().freshwaterLid(x,z);}
    public boolean polarClimateEnabled(){return continentalIceEnabled;}
    /** Datum for rock-relative underground rules (deep dark, underground structures, cave
     * entry routes, dripstone and sulfur habitat windows via {@code TerrainProfile.habitatGround};
     * cave approaches instead exclude sheet columns). A grounded continental sheet is new
     * overburden, not uplifted rock, so under it this is the complete original sample,
     * including any buried water; everywhere else it is exactly {@link #sample}. Legacy
     * alpine caps stay part of the published surface. Never differs without a sheet. */
    Sample bedSample(int x,int z){return continentalIceThickness(x,z)>0?iceReference().sample(x,z):sample(x,z);}
    public int bedHeight(int x,int z){var s=sample(x,z);int ice=continentalIceThickness(x,z);return ice>0?s.height()-ice:s.height();}
    /** Original complete surface, including erosion/coasts; eligibility never feeds back from new relief. */
    DrainageNetwork gravelReference() {
        if(!gravelReliefEnabled)return this;
        var found=unrelieved;if(found!=null)return found;
        synchronized(this){if(unrelieved==null)unrelieved=new DrainageNetwork(this,erodedLandforms);return unrelieved;}
    }
    private GravelDesertRelief gravelRelief() {
        var found=gravelRelief;if(found!=null)return found;
        synchronized(this){if(gravelRelief==null)gravelRelief=new GravelDesertRelief(seed,gravelReference());return gravelRelief;}
    }
    DrainageNetwork preformation() {
        if(!erodedLandforms)return this;
        var found=preformation;if(found!=null)return found;
        synchronized(this){if(preformation==null)preformation=new DrainageNetwork(this);return preformation;}
    }
    boolean erodedInterior(int x,int z){return erodedLandforms&&eroded().contains(x,z);}
    ErodedBadlands eroded() {
        var found=eroded;if(found!=null)return found;
        synchronized(this){if(eroded==null)eroded=new ErodedBadlands(seed,preformation());return eroded;}
    }

    public Sample sample(int x,int z) {
        long key=(x&0xffffffffL)|((long)z<<32);
        int slot=(int)mix(key)&(sampleCache.length-1);
        var found=sampleCache[slot];
        if(found!=null&&found.key==key)return found.value;
        // Concurrent misses may safely duplicate this
        // pure calculation. Structural relief callbacks bypass this cache.
        // The preformation view shares every base-field input and disables only
        // weathering. Reuse its exact immutable base sample instead of computing
        // it here and again in ErodedBadlands.envelope. Comparison mode stays raw.
        Sample value;
        if(gravelReliefEnabled&&!structuralComparison)value=gravelRelief().apply(x,z,gravelReference().sample(x,z));
        else {
            boolean weathered=erodedLandforms&&!structuralComparison;
            var base=weathered?preformation():this;
            // QA flags are captured per instance. A fixture may toggle them between
            // owner/view construction; preserve the old composition in that case.
            value=weathered&&!base.structuralComparison?base.sample(x,z):sample(x,z,!structuralComparison);
            if(weathered)value=eroded().apply(x,z,value);
        }
        if(deepBasinRelief!=null&&!structuralComparison)value=deepBasinRelief.apply(this,x,z,value);
        if(continentalIceThickness(x,z)>0)value=continentalIce().apply(x,z,value);
        else if(landIceEnabled&&!structuralComparison)value=landIce().apply(x,z,value);
        sampleCache[slot]=new CachedSample(key,value);
        return value;
    }
    /** Definition-3 structure atlas only: immutable broad mesh, no local relief/cache work. */
    long structureClimatePacked(int x,int z) {
        double gx=Math.clamp((x-(double)MIN)/STEP,0,SIDE-1.0),gz=Math.clamp((z-(double)MIN)/STEP,0,SIDE-1.0);
        int ix=Math.min((int)gx,SIDE-2),iz=Math.min((int)gz,SIDE-2);double tx=gx-ix,tz=gz-iz;
        int a=iz*SIDE+ix,b=a+SIDE;
        int height=(int)Math.floor((ground[a]*(1-tx)+ground[a+1]*tx)*(1-tz)+(ground[b]*(1-tx)+ground[b+1]*tx)*tz);
        int nearest=(tz<.5?iz:iz+1)*SIDE+(tx<.5?ix:ix+1),basin=root[nearest];
        int surface=height+1<TerrainShape.SEA_LEVEL&&ocean[basin]?TerrainShape.SEA_LEVEL:DRY;
        return ((long)height<<32)|(surface==DRY?0:1)|(ocean[basin]?2:0);
    }
    Sample structureClimateSample(int x,int z){long p=structureClimatePacked(x,z);return new Sample((int)(p>>32),(p&1)==0?DRY:TerrainShape.SEA_LEVEL,0,(p&2)==0);}

    /** Unchanged structural field used as the regression negative control. */
    Sample structuralSample(int x,int z) { return sample(x,z,false); }
    private Sample sample(int x, int z, boolean detailed) {
        int raw = shape.height(x, z), height = raw; double dryFloor = TerrainShape.SEA_LEVEL-1, riverNear=0, coastalRiverClearance=Double.POSITIVE_INFINITY;
        int cellX = Math.clamp(Math.floorDiv(x - MIN, STEP), 0, SIDE - 2);
        int cellZ = Math.clamp(Math.floorDiv(z - MIN, STEP), 0, SIDE - 2);
        int nearest = -1;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (int nz = cellZ; nz <= cellZ + 1; nz++) for (int nx = cellX; nx <= cellX + 1; nx++) {
            int i = nz * SIDE + nx;
            double d = square(x - nodeX(i)) + square(z - nodeZ(i));
            if (d < nearestDistance) { nearestDistance = d; nearest = i; }
        }
        int basin = root[nearest], legacyBasin=basin;
        int waterLevel = raw + 1 < TerrainShape.SEA_LEVEL && ocean[basin] ? TerrainShape.SEA_LEVEL : DRY;
        // A four-by-four neighborhood includes jittered endpoints, meanders,
        // valley shoulders, and lake footprints across every mesh/chunk seam.
        for (int nz = Math.max(0, cellZ - 1); nz <= Math.min(SIDE - 1, cellZ + 2); nz++)
            for (int nx = Math.max(0, cellX - 1); nx <= Math.min(SIDE - 1, cellX + 2); nx++) {
                int i = nz * SIDE + nx;
                if (parent[i] == -1 && !ocean[i]) {
                    double distance = PlanarMath.length(x - nodeX(i), z - nodeZ(i));
                    double angle = Math.atan2(z - nodeZ(i), x - nodeX(i));
                    double radius = lakeRadius(i) * shoreScale(i, angle);
                    double lakeGuard=1-smooth(Math.max(0,(distance-radius-4)/124));
                    if(lakeGuard>0)dryFloor=Math.max(dryFloor,water[i]-1-52*(1-lakeGuard)/lakeGuard);
                    if (distance < radius) {
                        double t = smooth(distance / radius);
                        int bowl = (int)Math.floor(lerp(t, water[i] - 18, raw));
                        height = Math.min(height, bowl);
                        if (bowl + 1 < water[i]) waterLevel = mergeWater(waterLevel, water[i]);
                        basin = i;legacyBasin=i;
                    }
                }
                if (!river(i)) continue;
                int p = parent[i];
                if (beyondRiverReach(i, p, x, z)) continue;
                double t = closestT(i, x, z);
                double baseWidth=lerp(t,halfWidth(i),halfWidth(p));
                double unshiftedDistance=PlanarMath.length(x-edgeX(i,t),z-edgeZ(i,t));
                // Keep coastal uplift outside the entire original carving/reservation envelope,
                // with an extra128m untouched buffer and a256m smooth falloff beyond it.
                coastalRiverClearance=Math.min(coastalRiverClearance,unshiftedDistance-Math.max(4.14*baseWidth+96,1.38*baseWidth+176)-128);
                // Preserve dry catchment labels independently of widened or
                // shifted carving shoulders. Wet columns still follow their route.
                if(unshiftedDistance<baseWidth*3+48)legacyBasin=root[i];
                // Conservative envelope includes maximum width growth, lateral
                // offset and the entire water-reservation falloff.
                if(detailed&&unshiftedDistance>=Math.max(4.14*baseWidth+96,1.38*baseWidth+176))continue;
                double rework=detailed?channelFormStrength(i,t):0;
                double width=channelWidth(i,t,detailed,rework);
                double distance=channelDistance(i,t,x,z,detailed,rework,width);
                double form=detailed?shape.bankForm(edgeX(i,t),edgeZ(i,t)):0;
                double outer=width*3+48;
                if (distance >= Math.max(outer,width+128)) continue;
                int level = (int)Math.floor(lerp(t, water[i], water[p]));
                // All branches share a flat confluence pool. Otherwise the
                // overlapping upstream ribbon can make the outgoing river rise.
                if (PlanarMath.length(x - nodeX(i), z - nodeZ(i)) < halfWidth(i) * 4) level = water[i];
                if (PlanarMath.length(x - nodeX(p), z - nodeZ(p)) < halfWidth(p) * 4) level = water[p];
                double riverGuard=1-smooth(Math.max(0,(distance-width-4)/124));
                riverNear=Math.max(riverNear,riverGuard);
                if(riverGuard>0)dryFloor=Math.max(dryFloor,level-1-52*(1-riverGuard)/riverGuard);
                if(distance>=outer)continue;
                double depth = 2 + Math.min(12, Math.log1p(area[i]) * 0.8);
                double bed;
                if(!detailed) bed=distance<=width?level-depth+depth*square(distance/width):level+(distance-width)*.2;
                else {
                    // A shallow bar on one bank and steeper cut bank on the other.
                    // Along-reach changes bend the wetted edge without moving the
                    // connected centreline or altering any downstream water datum.
                    double dx=edgeX(i,Math.min(1,t+.001))-edgeX(i,Math.max(0,t-.001));
                    double dz=edgeZ(i,Math.min(1,t+.001))-edgeZ(i,Math.max(0,t-.001));
                    double side=Math.signum(dx*(z-edgeZ(i,t))-dz*(x-edgeX(i,t)));
                    double run=8+42*(.5+.5*Math.tanh(3*side*form));
                    bed=distance<=width?level-1-depth*(1-Math.pow(distance/width,1.6+.5*form))
                        :raw-(raw-level+1)*Math.exp(-(distance-width)/run);
                }
                if(detailed)bed=lerp(rework,distance<=width?level-depth+depth*square(distance/width):level+(distance-width)*.2,bed);
                double blend = 1 - smooth(Math.max(0, (distance - width) / (outer - width)));
                int carved = (int)Math.floor(lerp(blend, raw, bed));
                height = Math.min(height, carved);
                if (distance < width && carved + 1 < level) waterLevel = mergeWater(waterLevel, level);
                basin = root[i];
            }
        if ((long)height + 1 >= waterLevel) waterLevel = DRY;
        if(detailed) {
            double limit=waterLevel==DRY?Math.min(120,2+Math.max(0,height-dryFloor)*.65):Math.min(26,(waterLevel-1-height)*.35);
            // Submerged relief has its own intrinsically bounded field. Clipping
            // large upland ridges to shallow depth caps would restore flat bands.
            double detail=waterLevel==DRY?surfaceRelief.offset(x,z,shape):2*limit*surfaceRelief.bankFraction(x,z,shape);
            // Keep the accepted bank field and fade, with two-thirds of its dry-bank amplitude.
            // Wet beds and non-bank upland relief retain their previous definition.
            if(waterLevel==DRY){double bankBlend=riverNear*(1-smooth(Math.max(0,(height-dryFloor-8)/24)));if(bankBlend>0)detail=detail*(1-bankBlend)+(2.0/3.0)*Math.min(26,Math.max(0,height-dryFloor))*surfaceRelief.bankFraction(x,z,shape)*bankBlend;}
            // Limit changes by water depth and retain at least half dry-bank clearance.
            // Smooth guard falloff avoids new cliffs at lake/channel reservation edges.
            int changed=(int)Math.round(height+Math.clamp(detail,waterLevel==DRY?-Math.min(120,Math.max(0,height-dryFloor)*.5):-limit,limit));
            // Relief retains the carved fluid footprint and datums. The tapered downward limit
            // prevents new datum-flat shelves without a hard height clamp.
            height=waterLevel==DRY?changed:Math.min(waterLevel-2,changed);
            if(height<=TerrainShape.FLOOR||height>=TerrainShape.CEILING)throw new IllegalStateException("Surface relief exceeds terrain envelope: "+height);
        }
        if(waterLevel==DRY)basin=legacyBasin;
        var result=new Sample(height,waterLevel,basin,!ocean[basin]);
        if(detailed&&sandyCoasts)result=sandyCoast.apply(x,z,result,coastalRiverClearance);
        return detailed&&coastalLandforms?CoastalLandforms.apply(seed,shape,this,x,z,result,coastalRiverClearance):result;
    }

    /** Necessary condition for a hut's own-lake dry bank, using the exact lake bowl.
     * Inside this lake, carving never exceeds bowl and dryFloor >= water-1.
     * Dry relief can therefore raise a submerged bowl by at most2; wet relief
     * stays below its water datum. Other carving/erosion only lowers it, and a
     * qualifying closed bank bypasses coastal uplift. This rejects deep water
     * without evaluating the expensive river/relief field; accepted points still
     * pass the complete unchanged Swamp predicate and block-resolution fit. */
    boolean couldBeTerminalLakeBank(int lake,int x,int z){
        int cx=Math.clamp(Math.floorDiv(x-MIN,STEP),0,SIDE-2),cz=Math.clamp(Math.floorDiv(z-MIN,STEP),0,SIDE-2);
        int lx=lake%SIDE,lz=lake/SIDE;
        if(parent[lake]!=-1||ocean[lake]||lx<Math.max(0,cx-1)||lx>Math.min(SIDE-1,cx+2)||lz<Math.max(0,cz-1)||lz>Math.min(SIDE-1,cz+2))return true;
        double distance=PlanarMath.length(x-nodeX(lake),z-nodeZ(lake));
        double radius=lakeRadius(lake)*shoreScale(lake,Math.atan2(z-nodeZ(lake),x-nodeX(lake)));
        if(distance>=radius)return true;
        int bowl=(int)Math.floor(lerp(smooth(distance/radius),water[lake]-18,shape.height(x,z)));
        return bowl>=water[lake]-3;
    }
    /** Identify actual terminal-lake water, not every river in its closed catchment. */
    boolean terminalLakeWater(int x, int z, Sample sample) {
        int lake = sample.basin();
        if (parent[lake] != -1 || ocean[lake] || sample.waterLevel() != water[lake]) return false;
        double angle = Math.atan2(z - nodeZ(lake), x - nodeX(lake));
        return PlanarMath.length(x - nodeX(lake), z - nodeZ(lake)) < lakeRadius(lake) * shoreScale(lake, angle);
    }

    double lakeShoreRadius(int lake, double angle) { return lakeRadius(lake) * shoreScale(lake, angle); }

    /** Read-only proximity to the existing routed curve; never carves or reroutes terrain. */
    public record Channel(int edge,int waterLevel,double halfWidth,double distance,double gradient) {
        static final Channel NONE=new Channel(-1,DRY,0,0,0);
    }
    public Channel channelAt(int x,int z) {
        int cellX=Math.clamp(Math.floorDiv(x-MIN,STEP),0,SIDE-2),cellZ=Math.clamp(Math.floorDiv(z-MIN,STEP),0,SIDE-2);
        Channel best=Channel.NONE; double score=Double.POSITIVE_INFINITY;
        for(int nz=Math.max(0,cellZ-1);nz<=Math.min(SIDE-1,cellZ+2);nz++)
            for(int nx=Math.max(0,cellX-1);nx<=Math.min(SIDE-1,cellX+2);nx++) {
                int i=nz*SIDE+nx;
                // A closed catchment's inflow is a river, but its lake footprint
                // belongs to the lake habitat even where curves overlap it.
                if(parent[i]==-1 && !ocean[i]) {
                    double angle=Math.atan2(z-nodeZ(i),x-nodeX(i));
                    if(PlanarMath.length(x-nodeX(i),z-nodeZ(i))<lakeRadius(i)*shoreScale(i,angle)) return Channel.NONE;
                }
                if(!river(i)) continue;
                int p=parent[i];
                // Both modes discard an edge whose centerline is beyond 2.7*baseWidth+48, and baseWidth
                // never exceeds the wider endpoint; the bounding box bounds that distance from below.
                double reach=2.7*Math.max(halfWidth(i),halfWidth(p))+48+1;
                if(edgeBoxDistanceSquared(i,p,x,z)>=reach*reach)continue;
                double t=closestT(i,x,z);
                double baseWidth=lerp(t,halfWidth(i),halfWidth(p));
                if(!structuralComparison&&PlanarMath.length(x-edgeX(i,t),z-edgeZ(i,t))>2.7*baseWidth+48)continue;
                double strength=structuralComparison?0:channelFormStrength(i,t);
                double width=channelWidth(i,t,!structuralComparison,strength),distance=channelDistance(i,t,x,z,!structuralComparison,strength,width);
                if(distance>width*1.4 || distance/width>=score) continue;
                int level=(int)Math.floor(lerp(t,water[i],water[p]));
                if(PlanarMath.length(x-nodeX(i),z-nodeZ(i))<halfWidth(i)*4) level=water[i];
                if(PlanarMath.length(x-nodeX(p),z-nodeZ(p))<halfWidth(p)*4) level=water[p];
                score=distance/width;
                best=new Channel(i,level,width,distance,(water[i]-water[p])/edgeLength(i));
            }
        return best;
    }
    double riverHalfWidth(int i) { return halfWidth(i); }
    private double channelWidth(int i,double t,boolean detailed){return channelWidth(i,t,detailed,detailed?channelFormStrength(i,t):0);}
    private double channelWidth(int i,double t,boolean detailed,double strength){
        double width=lerp(t,halfWidth(i),halfWidth(parent[i]));
        // Fixed node widths join tributaries; variation fades smoothly into the
        // existing confluence pools, avoiding gaps at graph junctions.
        double end=Math.min(t,1-t)*edgeLength(i),fade=smooth(Math.min(1,end/320));
        return detailed?width*(1+fade*strength*(shape.channelWidthScale(edgeX(i,t),edgeZ(i,t))-1)):width;
    }

    double channelDistance(int i,double t,int x,int z){return channelDistance(i,t,x,z,!structuralComparison);}
    private double channelDistance(int i,double t,int x,int z,boolean detailed){double strength=detailed?channelFormStrength(i,t):0;return channelDistance(i,t,x,z,detailed,strength,channelWidth(i,t,detailed,strength));}
    private double channelDistance(int i,double t,int x,int z,boolean detailed,double strength,double localWidth){
        double cx=edgeX(i,t),cz=edgeZ(i,t);
        if(!detailed)return PlanarMath.length(x-cx,z-cz);
        double fade=smooth(Math.min(1,Math.min(t,1-t)*edgeLength(i)/320));
        if(strength==0||fade==0)return PlanarMath.length(x-cx,z-cz);
        double dx=edgeX(i,Math.min(1,t+.001))-edgeX(i,Math.max(0,t-.001)),dz=edgeZ(i,Math.min(1,t+.001))-edgeZ(i,Math.max(0,t-.001)),length=PlanarMath.length(dx,dz);
        double shift=Math.min(48,.5*localWidth)*shape.channelShift(cx,cz)*fade*strength;
        return PlanarMath.length(x-cx+dz/length*shift,z-cz-dx/length*shift);
    }
    private double channelFormStrength(int i,double t){
        double level=lerp(t,water[i],water[parent[i]]);
        double incision=shape.continuousHeight(edgeX(i,t),edgeZ(i,t))-level;
        return 1-smooth((incision-32)/64);
    }

    /** Habitat definition 1 follows the same ocean-basin owner that creates sea
     * water in sample(). The nearest mesh node may be land while its downstream
     * root is ocean. Keep geometry, sandy-coast shaping and structural planners
     * on oceanWater(): this opt-in changes surface ecology, not those contracts. */
    boolean habitatOceanWater(int x,int z,Sample sample) {
        if(!surfaceOwnership)return oceanWater(x,z,sample);
        return sample.waterLevel()==TerrainShape.SEA_LEVEL && sample.height()+1<TerrainShape.SEA_LEVEL
            && !sample.closed() && shape.height(x,z)<TerrainShape.SEA_LEVEL-1 && ocean[sample.basin()];
    }
    boolean correctedHabitatOwnership(){return surfaceOwnership;}

    /** Sea water over raw seabed in the edge-connected ocean mesh, not a river or lake datum. */
    boolean oceanWater(int x,int z,Sample sample) {
        if(sample.waterLevel()!=TerrainShape.SEA_LEVEL || sample.closed() || shape.height(x,z)>=TerrainShape.SEA_LEVEL-1) return false;
        int cx=Math.clamp(Math.floorDiv(x-MIN,STEP),0,SIDE-2),cz=Math.clamp(Math.floorDiv(z-MIN,STEP),0,SIDE-2);
        int nearest=-1;double distance=Double.POSITIVE_INFINITY;
        for(int nz=cz;nz<=cz+1;nz++) for(int nx=cx;nx<=cx+1;nx++) {
            int i=nz*SIDE+nx;double d=square(x-nodeX(i))+square(z-nodeZ(i));
            if(d<distance) { nearest=i;distance=d; }
        }
        return ocean[nearest];
    }
    CoastalHeadlands.Relief headland(int x,int z){return headlands.sample(x,z);}

    /** Signed nearest-node ownership margin in metres; zero at the old Voronoi boundary. */
    double oceanClearance(int x,int z){return openClearance(x,z,true);}
    double openCatchmentClearance(int x,int z){return openClearance(x,z,false);}
    private double openClearance(int x,int z,boolean marine){
        int cx=Math.clamp(Math.floorDiv(x-MIN,STEP),0,SIDE-2),cz=Math.clamp(Math.floorDiv(z-MIN,STEP),0,SIDE-2);
        double open=Double.POSITIVE_INFINITY,closed=Double.POSITIVE_INFINITY;
        for(int nz=Math.max(0,cz-1);nz<=Math.min(SIDE-1,cz+2);nz++)for(int nx=Math.max(0,cx-1);nx<=Math.min(SIDE-1,cx+2);nx++){
            int i=nz*SIDE+nx;double distance=PlanarMath.length(x-nodeX(i),z-nodeZ(i));
            if(marine?ocean[i]:ocean[root[i]])open=Math.min(open,distance);else closed=Math.min(closed,distance);
        }
        return closed-open;
    }

    int nodeGround(int i) { return ground[i]; }

    // Planar triangulation: one diagonal per mesh square, with no crossing graph edges.
    private static boolean neighbor(int x, int z, int dx, int dz) {
        if ((dx == 0 && dz == 0) || x + dx < 0 || x + dx >= SIDE || z + dz < 0 || z + dz >= SIDE) return false;
        return dx == 0 || dz == 0 || ((x + z) & 1) == 0;
    }
    int size() { return COUNT; }
    int parent(int i) { return parent[i]; }
    int outlet(int i) { return root[i]; }
    boolean closed(int i) { return !ocean[root[i]]; }
    int waterLevel(int i) { return water[i]; }
    int accumulation(int i) { return area[i]; }
    boolean river(int i) { return parent[i] >= 0 && !ocean[i] && area[i] >= RIVER_AREA; }
    int nodeX(int i) { return nodeXs[i]; }
    int nodeZ(int i) { return nodeZs[i]; }
    private int computeNodeX(int i) { return MIN + (i % SIDE) * STEP + jitter(i, false); }
    private int computeNodeZ(int i) { return MIN + (i / SIDE) * STEP + jitter(i, true); }
    private int jitter(int i, boolean z) {
        int axis = z ? i / SIDE : i % SIDE;
        if (axis == 0 || axis == SIDE - 1) return 0;
        return (int)((mix(seed ^ (i * 0x9E3779B97F4A7C15L) ^ (z ? 0xD1B54A32D192ED03L : 0)) >>> 11) * 0x1.0p-53 * STEP * 0.36 - STEP * 0.18);
    }
    double edgeX(int i, double t) {
        int p = parent[i];
        return bezier(nodeX(i), control(i, false, false), control(i, true, false), nodeX(p), t);
    }
    double edgeZ(int i, double t) {
        int p = parent[i];
        return bezier(nodeZ(i), control(i, false, true), control(i, true, true), nodeZ(p), t);
    }
    private double control(int i, boolean end, boolean zAxis) {
        int slot = edgeSlot[i];
        return slot >= 0 ? edgeControl[4 * slot + (end ? 2 : 0) + (zAxis ? 1 : 0)] : computeControl(i, end, zAxis);
    }
    private double computeControl(int i, boolean end, boolean zAxis) {
        int p = parent[i], node = end ? p : i;
        double dx = nodeX(p) - nodeX(i), dz = nodeZ(p) - nodeZ(i), length = PlanarMath.length(dx, dz);
        double tx = tangentX[node], tz = tangentZ[node];
        // A side tributary must still approach its endpoint from upstream,
        // even if the main stem's tangent points away from that tributary.
        if (tx * dx + tz * dz < length * 0.15) { tx = dx / length; tz = dz / length; }
        return (zAxis ? nodeZ(node) : nodeX(node)) + (end ? -1 : 1) * (zAxis ? tz : tx) * length * 0.33;
    }
    // Bernstein weights {(1-t)^3, 3(1-t)^2 t, 3(1-t)t^2, t^3} for the coarse scan at t=k/8, with
    // the same operation order as bezier() so the products are identical.
    private static final double[][] SCAN_WEIGHTS = new double[9][];
    static {
        for (int k = 0; k <= 8; k++) {
            double t = k / 8.0, s = 1 - t;
            SCAN_WEIGHTS[k] = new double[]{s * s * s, 3 * s * s * t, 3 * s * t * t, t * t * t};
        }
    }
    /** Squared distance from (x,z) to the cubic with control values a..d, from weights shared by both axes. */
    private static double curveDistance(double w0, double w1, double w2, double w3,
            double ax, double cx, double ex, double bx, double az, double cz, double ez, double bz, double x, double z) {
        return square(w0 * ax + w1 * cx + w2 * ex + w3 * bx - x) + square(w0 * az + w1 * cz + w2 * ez + w3 * bz - z);
    }
    private static double curveDistance(double t,
            double ax, double cx, double ex, double bx, double az, double cz, double ez, double bz, double x, double z) {
        double s = 1 - t;
        return curveDistance(s * s * s, 3 * s * s * t, 3 * s * t * t, t * t * t, ax, cx, ex, bx, az, cz, ez, bz, x, z);
    }
    /**
     * True when (x,z) is too far from river edge {@code i} for anything in {@link #sample} to depend
     * on it. The curve lies inside the bounding box of its four control points, so that box bounds
     * the distance from below. Past the carving envelope {@code max(4.14w+96, 1.38w+176)} an edge
     * changes neither height, water, basin nor the dry catchment label, and its coast clearance
     * ({@code distance - envelope - 128}) is at least the 256 m beyond which both consumers
     * ({@code smooth(0,256,..)} and a {@code <= 0} test) are saturated. {@code w} is bounded by the
     * wider endpoint, the envelope grows with it, and one extra metre absorbs rounding.
     */
    private boolean beyondRiverReach(int i, int p, int x, int z) {
        double w = Math.max(halfWidth(i), halfWidth(p));
        double reach = Math.max(4.14 * w + 96, 1.38 * w + 176) + 128 + 256 + 1;
        return edgeBoxDistanceSquared(i, p, x, z) >= reach * reach;
    }
    /** Squared distance from (x,z) to the bounding box of river edge {@code i}'s four control points. */
    private double edgeBoxDistanceSquared(int i, int p, int x, int z) {
        int k = 4 * edgeSlot[i];
        double cx = edgeControl[k], cz = edgeControl[k + 1], ex = edgeControl[k + 2], ez = edgeControl[k + 3];
        double ax = nodeX(i), az = nodeZ(i), bx = nodeX(p), bz = nodeZ(p);
        double dx = Math.max(Math.max(Math.min(Math.min(ax, bx), Math.min(cx, ex)) - x, x - Math.max(Math.max(ax, bx), Math.max(cx, ex))), 0);
        double dz = Math.max(Math.max(Math.min(Math.min(az, bz), Math.min(cz, ez)) - z, z - Math.max(Math.max(az, bz), Math.max(cz, ez))), 0);
        return dx * dx + dz * dz;
    }
    double closestT(int i, double x, double z) {
        int p = parent[i];
        double ax = nodeX(i), az = nodeZ(i), bx = nodeX(p), bz = nodeZ(p);
        double cx = control(i, false, false), cz = control(i, false, true);
        double ex = control(i, true, false), ez = control(i, true, true);
        double bestT = 0, bestDistance = Double.POSITIVE_INFINITY;
        for (int k = 0; k <= 8; k++) {
            double[] w = SCAN_WEIGHTS[k];
            double distance = curveDistance(w[0], w[1], w[2], w[3], ax, cx, ex, bx, az, cz, ez, bz, x, z);
            if (distance < bestDistance) { bestDistance = distance; bestT = k / 8.0; }
        }
        // A bounded search is reliable at sharp bends where an unconstrained
        // Newton projection can jump to an endpoint and leave dry channel gaps.
        double lo = Math.max(0, bestT - 0.125), hi = Math.min(1, bestT + 0.125);
        final double ratio = 0.6180339887498949;
        double u = hi - ratio * (hi - lo), v = lo + ratio * (hi - lo);
        double du = curveDistance(u, ax, cx, ex, bx, az, cz, ez, bz, x, z);
        double dv = curveDistance(v, ax, cx, ex, bx, az, cz, ez, bz, x, z);
        for (int k = 0; k < 22; k++) {
            if (du < dv) {
                hi = v; v = u; dv = du; u = hi - ratio * (hi - lo);
                du = curveDistance(u, ax, cx, ex, bx, az, cz, ez, bz, x, z);
            } else {
                lo = u; u = v; du = dv; v = lo + ratio * (hi - lo);
                dv = curveDistance(v, ax, cx, ex, bx, az, cz, ez, bz, x, z);
            }
        }
        double t = (lo + hi) * 0.5;
        double distance = curveDistance(t, ax, cx, ex, bx, az, cz, ez, bz, x, z);
        return bestDistance < distance ? bestT : t;
    }
    private static double bezier(double a, double b, double c, double d, double t) {
        double s = 1 - t;
        return s * s * s * a + 3 * s * s * t * b + 3 * s * t * t * c + t * t * t * d;
    }
    private double shoreScale(int i, double angle) {
        double phase = (mix(seed ^ i ^ 731) >>> 11) * 0x1.0p-53 * 2 * Math.PI;
        return 1 + 0.22 * Math.sin(2 * angle + phase) + 0.15 * Math.sin(3 * angle - phase)
            + 0.08 * Math.sin(5 * angle + phase * 2);
    }
    /** Lowest terrain height along the curved route of river edge {@code i}, less the 16 m freeboard. */
    private int routeClearance(int i) {
        int limit = Integer.MAX_VALUE;
        int steps = Math.max(1, (int)(edgeLength(i) / 64));
        for (int k = 0; k <= steps; k++) {
            double t = k / (double)steps;
            limit = Math.min(limit, shape.height((int)Math.round(edgeX(i, t)), (int)Math.round(edgeZ(i, t))) - 16);
        }
        return limit;
    }
    private double edgeLength(int i) { return PlanarMath.length(nodeX(parent[i]) - nodeX(i), nodeZ(parent[i]) - nodeZ(i)); }
    private double halfWidth(int i) { return 3 + Math.min(400, Math.sqrt(area[i]) * 1.8); }
    private double lakeRadius(int i) { return STEP * (0.25 + (mix(seed ^ i ^ 19) >>> 11) * 0x1.0p-53 * 0.12); }
    // Connected water drains to the lower overlapping surface at a merge;
    // taking the higher tributary level creates an uphill lip on the main stem.
    private static int mergeWater(int current, int level) { return current == DRY ? level : Math.min(current, level); }
    private static double square(double x) { return x * x; }
    private static double smooth(double t) { t = Math.clamp(t, 0, 1); return t * t * (3 - 2 * t); }
    private static double lerp(double t, double a, double b) { return a + t * (b - a); }
    private static long mix(long h) {
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    /** Primitive heap avoids a million boxed queue entries during construction. */
    private final class Heap {
        private final int[] nodes = new int[COUNT], costs;
        private int size;
        Heap(int[] costs) { this.costs = costs; }
        boolean empty() { return size == 0; }
        private boolean less(int a, int b) {
            if (costs[a] != costs[b]) return costs[a] < costs[b];
            return Long.compareUnsigned(mix(seed ^ a), mix(seed ^ b)) < 0;
        }
        void add(int node) {
            int i = size++;
            while (i > 0) {
                int p = (i - 1) >>> 1;
                if (!less(node, nodes[p])) break;
                nodes[i] = nodes[p]; i = p;
            }
            nodes[i] = node;
        }
        int remove() {
            int result = nodes[0], last = nodes[--size], i = 0;
            while (i * 2 + 1 < size) {
                int child = i * 2 + 1;
                if (child + 1 < size && less(nodes[child + 1], nodes[child])) child++;
                if (!less(nodes[child], last)) break;
                nodes[i] = nodes[child]; i = child;
            }
            if (size > 0) nodes[i] = last;
            return result;
        }
    }
}
