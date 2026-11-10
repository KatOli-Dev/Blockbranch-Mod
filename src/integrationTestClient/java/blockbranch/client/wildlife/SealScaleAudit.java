package blockbranch.client.wildlife;
import blockbranch.Blockbranch;
import blockbranch.wildlife.HarborSeal;
import net.minecraft.client.Minecraft;

/** Native pose-stack and baked geometry measurement, independent of perspective. */
public final class SealScaleAudit {
    static void verifyMesh(){
        var root=SealModel.layer().bakeRoot();var model=new SealModel(root);
        root.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{
            for(var face:cube.polygons)for(var v:face.vertices())
                if(!corner(v.x(),cube.minX,cube.maxX)||!corner(v.y(),cube.minY,cube.maxY)||!corner(v.z(),cube.minZ,cube.maxZ))
                    throw new AssertionError("Seal must retain plain cuboid geometry");
        });
        var state=new SealRenderer.State();model.setupAnim(state);var adult=points(root);
        state.isBaby=true;state.activity=2;model.setupAnim(state);
        state.isBaby=false;state.activity=0;model.setupAnim(state);var reset=points(root);
        if(adult.size()!=reset.size())throw new AssertionError("Seal mesh changed during animation");
        for(int n=0;n<adult.size();n++)if(Math.abs(adult.get(n)-reset.get(n))>1e-6)throw new AssertionError("Seal pose/scale did not reset");
        Blockbranch.LOGGER.info("SEAL_STYLE_POSE_PASS plainCuboids=true adultBabyRestAdultReset=true");
    }
    private static boolean corner(float value,float low,float high){return Math.abs(value-low)<1e-6||Math.abs(value-high)<1e-6;}
    private static java.util.List<Float> points(net.minecraft.client.model.geom.ModelPart root){
        var points=new java.util.ArrayList<Float>();
        root.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{
            for(var face:cube.polygons)for(var vertex:face.vertices()){
                var p=pose.pose().transformPosition(new org.joml.Vector3f(vertex.worldX(),vertex.worldY(),vertex.worldZ()));
                points.add(p.x);points.add(p.y);points.add(p.z);
            }
        });return points;
    }
    public static void measure(Minecraft client,HarborSeal animal){
        var renderer=(SealRenderer)client.getEntityRenderDispatcher().getRenderer(animal);
        var state=renderer.createRenderState();renderer.extractRenderState(animal,state,0);state.xRot=0;state.yRot=0;state.activity=0;
        var root=SealModel.layer().bakeRoot();new SealModel(root).setupAnim(state);
        var pose=new com.mojang.blaze3d.vertex.PoseStack();pose.scale(state.scale,state.scale,state.scale);pose.scale(-1,-1,1);renderer.scale(state,pose);pose.translate(0,-1.501F,0);
        float[] bounds={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
        root.visit(pose,(matrix,path,index,cube)->{
            for(float x:new float[]{cube.minX,cube.maxX})for(float y:new float[]{cube.minY,cube.maxY})for(float z:new float[]{cube.minZ,cube.maxZ}){
                var point=matrix.pose().transformPosition(new org.joml.Vector3f(x/16,y/16,z/16));
                for(int axis=0;axis<3;axis++){bounds[axis]=Math.min(bounds[axis],point.get(axis));bounds[axis+3]=Math.max(bounds[axis+3],point.get(axis));}
            }
        });
        float age=animal.isBaby()?.5F:1;
        if(Math.abs(state.scale-1)>.00001||Math.abs(state.ageScale-age)>.00001||Math.abs(bounds[1]-.0009F*age)>.00001
            ||Math.abs(bounds[4]-bounds[1]-.45F*age)>.0001||Math.abs(bounds[3]-bounds[0]-.9F*age)>.0001||Math.abs(bounds[5]-bounds[2]-1.659375F*age)>.0001)
            throw new AssertionError("Seal scale conversion changed "+java.util.Arrays.toString(bounds));
        Blockbranch.LOGGER.info("SEAL_SCALE_PASS baby={} attributeScale={} ageScale={} bounds={} width={} height={} length={} hitbox={}x{} feetY={}",
            animal.isBaby(),state.scale,state.ageScale,java.util.Arrays.toString(bounds),bounds[3]-bounds[0],bounds[4]-bounds[1],bounds[5]-bounds[2],animal.getBbWidth(),animal.getBbHeight(),animal.getY());
    }
    private SealScaleAudit(){}
}
