package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.Mallard;
import net.minecraft.client.Minecraft;

/** Native pose-stack and baked geometry measurement, independent of perspective. */
public final class MallardScaleAudit {
    public static void measure(Minecraft client,Mallard animal){
        var renderer=(MallardRenderer)client.getEntityRenderDispatcher().getRenderer(animal);
        var state=renderer.createRenderState();renderer.extractRenderState(animal,state,0);state.xRot=0;state.yRot=0;state.activity=0;
        var root=MallardModel.layer().bakeRoot();new MallardModel(root).setupAnim(state);
        var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.scale(state.scale,state.scale,state.scale);pose.scale(-1,-1,1);renderer.scale(state,pose);pose.translate(0,-1.501F,0);
        float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        root.visit(pose,(matrix,path,index,cube)->{
            for(float x:new float[]{cube.minX,cube.maxX})for(float y:new float[]{cube.minY,cube.maxY})for(float z:new float[]{cube.minZ,cube.maxZ}){
                var point=matrix.pose().transformPosition(new org.joml.Vector3f(x/16,y/16,z/16));
                for(int axis=0;axis<3;axis++){bounds[axis]=Math.min(bounds[axis],point.get(axis));bounds[axis+3]=Math.max(bounds[axis+3],point.get(axis));}
            }
        });
        float age=animal.isBaby()?.5F:1;
        if(Math.abs(state.scale-1)>.00001||Math.abs(state.ageScale-age)>.00001||Math.abs(bounds[1]-.00065F*age)>.00001
            ||Math.abs(bounds[4]-bounds[1]-.4875F*age)>.0001||Math.abs(bounds[3]-bounds[0]-.40625F*age)>.0001||Math.abs(bounds[5]-bounds[2]-.690625F*age)>.0001)
            throw new AssertionError("Mallard scale conversion changed "+java.util.Arrays.toString(bounds));
        Blockbranch.LOGGER.info("MALLARD_SCALE_PASS baby={} attributeScale={} ageScale={} bounds={} width={} height={} length={} hitbox={}x{} feetY={}",
            animal.isBaby(),state.scale,state.ageScale,java.util.Arrays.toString(bounds),bounds[3]-bounds[0],bounds[4]-bounds[1],bounds[5]-bounds[2],animal.getBbWidth(),animal.getBbHeight(),animal.getY());
    }
    private MallardScaleAudit(){}
}
