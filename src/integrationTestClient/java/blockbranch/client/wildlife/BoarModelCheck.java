package blockbranch.client.wildlife;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
final class BoarModelCheck extends net.minecraft.client.model.animal.pig.PigModel {
    private BoarModelCheck(ModelPart root){super(root);}
    private static void verifyAcceptedGeometry(ModelPart current) {
        var mesh=createBasePigModel(CubeDeformation.NONE);
        var reference=mesh.getRoot().addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0)
            .addBox(-4,-4,-8,8,8,8).texOffs(16,16).addBox(-3,0,-11,6,4,3),PartPose.offset(0,12,-6));
        reference.addOrReplaceChild("tusks",CubeListBuilder.create().texOffs(40,48)
            .addBox(-4,1,-11,2,2,2).addBox(2,1,-11,2,2,2),PartPose.ZERO);
        var accepted=LayerDefinition.create(mesh,64,64).bakeRoot();
        var expected=bounds(accepted);var actual=bounds(current);
        if(expected.size()!=9||!expected.keySet().equals(actual.keySet()))throw new AssertionError("Accepted boar cuboid inventory changed");
        for(var key:expected.keySet())for(int i=0;i<6;i++){
            float bias=key.contains("/tusks:")&&(i==2||i==5)?-.04F/16:0;
            if(key.contains("_leg:")&&(i==0||i==3))bias=(key.startsWith("/left")?-.025F:.025F)/16;
            if(Math.abs(expected.get(key)[i]+bias-actual.get(key)[i])>.00001F)throw new AssertionError("Accepted boar geometry changed at "+key);
        }
        blockbranch.Blockbranch.LOGGER.info("BOAR_ACCEPTED_SHAPE_PASS reference=dc6d481 cuboids=9 standingBodyHeadUnchanged=true tuskDepthBiasModelUnits=0.04 legInsetModelUnits=0.025");
    }
    private static java.util.Map<String,float[]> bounds(ModelPart root) {
        var result=new java.util.TreeMap<String,float[]>();
        root.visit(new com.mojang.blaze3d.vertex.PoseStack(),(pose,path,index,cube)->{
            float[] range={Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.POSITIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY,Float.NEGATIVE_INFINITY};
            for(float x:new float[]{cube.minX,cube.maxX})for(float y:new float[]{cube.minY,cube.maxY})for(float z:new float[]{cube.minZ,cube.maxZ}){
                var point=pose.pose().transformPosition(new org.joml.Vector3f(x/16,y/16,z/16));
                for(int axis=0;axis<3;axis++){float value=point.get(axis);range[axis]=Math.min(range[axis],value);range[axis+3]=Math.max(range[axis+3],value);}
            }
            result.put(path+":"+index,range);
        });return result;
    }
    static void verifyRevision(){
        var root=BoarModel.layer().bakeRoot();verifyAcceptedGeometry(root);var model=new BoarModel(root);var state=new BoarRenderer.State();
        state.activity=2;state.xRot=35;state.yRot=0;
        model.setupAnim(state);var first=bounds(root);
        for(int i=0;i<40;i++)model.setupAnim(state);
        var current=bounds(root);float bottom=Float.NEGATIVE_INFINITY;
        for(var key:first.keySet())for(int i=0;i<6;i++)if(Math.abs(first.get(key)[i]-current.get(key)[i])>1e-5)throw new AssertionError("Boar rest transform accumulates");
        for(var box:current.values())bottom=Math.max(bottom,box[4]);
        if(Math.abs(bottom-1.5F)>1e-5||Math.abs(root.getChild("body").y+4-24)>1e-5)throw new AssertionError("Boar belly must meet native sole plane");
        state.activity=0;state.xRot=0;model.setupAnim(state);verifyAcceptedGeometry(root);
        blockbranch.Blockbranch.LOGGER.info("BOAR_POSE_REGRESSION PASS restingBottomModelY={} standingRestStanding=true noAccumulation=true",bottom*16);
    }
}
