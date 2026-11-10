package blockbranch.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

final class DeerModel extends EntityModel<DeerRenderer.State> {
    private final ModelPart body, neck, head, frontLeft, frontRight, backLeft, backRight;
    DeerModel(ModelPart root) {
        super(root);body=root.getChild("body");neck=root.getChild("neck");head=neck.getChild("head");
        frontLeft=root.getChild("front_left");frontRight=root.getChild("front_right");
        backLeft=root.getChild("back_left");backRight=root.getChild("back_right");
    }
    static LayerDefinition layer() {
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var body=root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0)
            .addBox(-4,-4,-7,8,8,15),PartPose.offset(0,9,0));
        body.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(64,24)
            .addBox(-1,0,-1,2,5,2),PartPose.offsetAndRotation(0,-1,8,.25F,0,0));
        var neck=root.addOrReplaceChild("neck",CubeListBuilder.create().texOffs(0,32)
            .addBox(-2,-8,-2,4,10,5,new CubeDeformation(-.25F,0,-.5F)),PartPose.offsetAndRotation(0,7,-5,.25F,0,0));
        // Finer facial sampling keeps compact eyes and separate nose/mouth marks
        // without changing either box, its deformation or the atlas dimensions.
        var head=neck.addOrReplaceChild("head",CubeListBuilder.create().texOffs(40,16)
            .addBox(-2,-2,-5,4,4,7,new CubeDeformation(-.025F,0,0),.5F,.5F).texOffs(40,32).addBox(-1.5F,0,-8,3,2,5,new CubeDeformation(-.025F,-.025F,0),.5F,.5F),PartPose.offset(0,-8,-1));
        for(int sign:new int[]{-1,1}) {
            head.addOrReplaceChild("ear"+sign,CubeListBuilder.create().texOffs(64,48)
                .addBox(-1,-4,-.5F,2,4,1),PartPose.offsetAndRotation(sign*2,-1,0,.15F,0,sign*.65F));
            addAntler(head,sign);
        }
        for(String name:new String[]{"front_left","front_right","back_left","back_right"}) {
            boolean front=name.startsWith("front"),left=name.endsWith("left");
            root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(64,0)
                .addBox(-1,0,-1,2,13,2),PartPose.offset(left?2.975F:-2.975F,11,front?-5:6));
        }
        return LayerDefinition.create(mesh,128,96);
    }
    /** Two beam sections and two tines keep each crown readable at game distance. */
    private static void addAntler(PartDefinition head,int sign) {
        var antler=head.addOrReplaceChild("antler"+sign,CubeListBuilder.create().texOffs(96,0)
            .addBox(-.625F,-5,-.625F,1.25F,5.15F,1.25F,new CubeDeformation(-.015F)),
            PartPose.offsetAndRotation(sign*1.7F,-1.8F,-.6F,-.18F,0,sign*.28F));
        var beam=antler.addOrReplaceChild("beam",CubeListBuilder.create().texOffs(96,0)
            .addBox(-.5F,-4.5F,-.5F,1,4.7F,1,new CubeDeformation(-.01F)),
            PartPose.offsetAndRotation(0,-4.8F,0,-.18F,0,sign*.12F));
        antler.addOrReplaceChild("brow_tine",CubeListBuilder.create().texOffs(96,0)
            .addBox(-.4F,-3.6F,-.4F,.8F,3.8F,.8F),
            PartPose.offsetAndRotation(0,-1.5F,-.4F,.95F,sign*.12F,0));
        beam.addOrReplaceChild("fork",CubeListBuilder.create().texOffs(96,0)
            .addBox(-.375F,-3.2F,-.375F,.75F,3.4F,.75F),
            PartPose.offsetAndRotation(0,-2,-.25F,.25F,sign*.1F,-sign*1.05F));
    }
    @Override public void setupAnim(DeerRenderer.State state) {
        super.setupAnim(state);
        head.yRot=state.yRot*Mth.DEG_TO_RAD;head.xRot=state.xRot*Mth.DEG_TO_RAD;
        if(state.isBaby){
            // A broad young face and compact torso distinguish a calf from a
            // uniformly scaled hind without adding joints or changing the coat.
            head.xScale=1.25F;head.yScale=1.2F;head.zScale=.9F;
            body.xScale=.95F;body.zScale=.9F;
        }
        boolean stag=state.stag&&!state.isBaby;
        head.getChild("antler-1").visible=head.getChild("antler1").visible=stag;
        // These long, rigid legs have no knee joint: bound their crossing at full speed.
        float stride=Mth.clamp(Mth.cos(state.walkAnimationPos*.75F)*state.walkAnimationSpeed*.9F,-.35F,.35F);
        frontLeft.xRot=backRight.xRot=stride;frontRight.xRot=backLeft.xRot=-stride;
        if(state.activity==1){
            // Lower the rigid neck from within the shoulder so the muzzle reaches grass.
            // Align the feeding head: a stale look yaw would sweep the antlers into the ground.
            neck.y+=4.8F;neck.xRot=1.9F+.04F*Mth.sin(state.ageInTicks*.25F);
            head.xRot=-.4F;head.yRot=0;
            // Keep the shorter juvenile muzzle at the existing grass contact height.
            if(state.isBaby)head.z-=.8F;
        }
        if(state.activity==2){
            body.y+=10.75F;body.getChild("tail").xRot=0;body.getChild("tail").y-=.025F;neck.y+=10.75F;
            // Reuse the four straight legs as short tucked limbs. Their roots stay
            // overlapping the belly edge; horizontal folds and inward hooves remain visible.
            // A small ground clearance avoids coincident sole/terrain faces.
            frontLeft.y=frontRight.y=backLeft.y=backRight.y=22.975F;
            frontLeft.yScale=frontRight.yScale=.45F;backLeft.yScale=backRight.yScale=.32F;
            frontLeft.x=backLeft.x=4.5F;frontRight.x=backRight.x=-4.5F;
            frontLeft.z=frontRight.z=-6.25F;backLeft.z=backRight.z=6.5F;
            frontLeft.xRot=frontRight.xRot=Mth.HALF_PI;backLeft.xRot=backRight.xRot=-Mth.HALF_PI;
        }
    }
}
