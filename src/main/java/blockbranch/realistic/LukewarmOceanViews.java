package blockbranch.realistic;

/** Shared fixture pose contract so the client waits for the requested server camera. */
public final class LukewarmOceanViews {
    public record View(int x,int z,double aboveBed,double yaw,double pitch) {}
    public static View at(int bedX,int bedZ,boolean second) {
        int radius=second?12:7;double angle=second?2.3:.8;
        int x=bedX+(int)Math.round(Math.cos(angle)*radius),z=bedZ+(int)Math.round(Math.sin(angle)*radius);
        return new View(x,z,second?3:1.25,Math.toDegrees(Math.atan2(-(bedX-x),bedZ-z)),second?16:8);
    }
    private LukewarmOceanViews(){}
}
