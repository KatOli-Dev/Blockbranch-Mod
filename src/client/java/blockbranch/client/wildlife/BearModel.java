package blockbranch.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Ten broad cuboids; shoulder hump, short ears and muzzle carry the bear silhouette. */
final class BearModel extends EntityModel<BearRenderer.State> {
    private final ModelPart body,hump,head,frontLeft,frontRight,backLeft,backRight;
    BearModel(ModelPart root){super(root);body=root.getChild("body");hump=root.getChild("hump");head=root.getChild("head");
        frontLeft=root.getChild("front_left");frontRight=root.getChild("front_right");backLeft=root.getChild("back_left");backRight=root.getChild("back_right");}
    static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-7,-7,-12,14,14,24),PartPose.offset(0,12,0));
        root.addOrReplaceChild("hump",CubeListBuilder.create().texOffs(0,40).addBox(-6,-2,-10,12,3,9),PartPose.offset(0,5,-1.975F));
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(80,0).addBox(-5,-4,-6,10,9,10)
            .texOffs(80,24).addBox(-3.5F,0,-10,7,4,4),PartPose.offset(0,11,-13));
        for(int sign:new int[]{-1,1})head.addOrReplaceChild("ear"+sign,CubeListBuilder.create().texOffs(112,24).addBox(-1.5F,-3,-1,3,3,2),PartPose.offset(sign*3,-4,0));
        for(String name:new String[]{"front_left","front_right","back_left","back_right"})root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,56)
            .addBox(-2.5F,0,-3,5,7,6),PartPose.offset(name.endsWith("left")?4.475F:-4.475F,17,name.startsWith("front")?-8:8));
        return LayerDefinition.create(mesh,128,96);
    }
    @Override public void setupAnim(BearRenderer.State state){
        super.setupAnim(state);head.yRot=state.yRot*Mth.DEG_TO_RAD;head.xRot=state.xRot*Mth.DEG_TO_RAD;
        // Slightly off a quarter scale to separate cub head/ear and torso planes.
        if(state.isBaby){head.xScale=1.24F;head.yScale=1.24F;head.zScale=1.24F;}
        float stride=Mth.cos(state.walkAnimationPos*.65F)*state.walkAnimationSpeed*.8F;
        frontLeft.xRot=backRight.xRot=stride;frontRight.xRot=backLeft.xRot=-stride;
        frontRight.xRot-=Mth.sin(state.attack*Mth.PI);frontLeft.xRot-=Mth.sin(state.attack*Mth.PI)*.35F;
        if(state.activity==1){
            // Lower the muzzle as the stride settles; keep the approach nod while
            // walking toward food. The enlarged cub head needs no downward offset.
            float reach=1-Mth.clamp(state.walkAnimationSpeed*4,0,1);
            float bob=Mth.sin(state.ageInTicks*.25F);
            if(!state.isBaby)head.y+=3*reach;
            head.xRot=Mth.lerp(reach,.45F+.04F*bob,.78F+.015F*bob);
        }
        if(state.activity==2){body.y+=5;hump.y+=5;head.y+=5;frontLeft.y+=2.25F;frontRight.y+=2.25F;backLeft.y+=2.25F;backRight.y+=2.25F;frontLeft.xRot=frontRight.xRot=-1.3F;backLeft.xRot=backRight.xRot=1.3F;}
    }
}
