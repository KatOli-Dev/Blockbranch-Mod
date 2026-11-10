package blockbranch.realistic;

/** Capture-only poses; never changes generated blocks or normal player views. */
public final class SeaIceCaptureViews {
    public static MountainPeaks.View view(int x,int y,int z,boolean second,java.util.function.IntBinaryOperator ground){
        if(!Boolean.getBoolean("blockbranch.seaIceUnderwater"))return blockbranch.realistic.FrozenOceanFixtures.view(x,y,z,second,ground);
        int floor=ground.applyAsInt(x,z);
        if(floor==Integer.MIN_VALUE)return null;
        double feet=second?floor+5:TerrainShape.SEA_LEVEL-5;
        return new MountainPeaks.View(x+.5,feet,z+.5,35,second?35:-25);
    }
    public static net.minecraft.core.BlockPos fixture(RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r){
        int kind=Boolean.getBoolean("blockbranch.seaIceDeep")?2:1;
        if(!Boolean.getBoolean("blockbranch.seaIceUnderwater"))return SeaIceSmoke.fixture(g,r);
        return underwaterFixture(g,r,kind);
    }
    static net.minecraft.core.BlockPos underwaterFixture(RealisticGenerator g,net.minecraft.world.level.levelgen.RandomState r,int kind){
        var d=g.drainage(r);var c=new ClimateField(r.seed());
        for(int i=0;i<d.size();i++){
            int x=d.nodeX(i),z=d.nodeZ(i);
            if(Math.abs(x)>2050000||Math.abs(z)>2050000||FrozenOcean.kind(r.seed(),x,z,d,c)!=kind||!FrozenOcean.ice(r.seed(),x,z,d,c)||g.icebergsEnabled()&&IcebergField.habitat(r.seed(),x,z,d,c))continue;
            boolean clear=true;
            for(int dz=-16;dz<=16;dz+=16)for(int dx=-16;dx<=16;dx+=16){var q=d.sample(x+dx,z+dz);if(q.waterLevel()!=64||64-q.height()-1<24)clear=false;}
            if(clear)return new net.minecraft.core.BlockPos(x,63,z);
        }
        throw new AssertionError("No underwater frozen marine witness kind="+kind);
    }
    private SeaIceCaptureViews(){}
}
