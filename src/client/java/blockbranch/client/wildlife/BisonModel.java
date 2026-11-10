package blockbranch.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.util.Mth;

/** Broad shoulder hump and low head, with a short pixel-cutout mane fringe. */
final class BisonModel extends EntityModel<BisonRenderer.State> {
    private final ModelPart head,shoulder,rump,frontLeft,frontRight,backLeft,backRight;
    BisonModel(ModelPart root){super(root);head=root.getChild("head");shoulder=root.getChild("shoulder");rump=root.getChild("rump");
        frontLeft=root.getChild("front_left");frontRight=root.getChild("front_right");backLeft=root.getChild("back_left");backRight=root.getChild("back_right");}
    static LayerDefinition layer(){
        var mesh=new MeshDefinition();var root=mesh.getRoot();
        var shoulder=root.addOrReplaceChild("shoulder",CubeListBuilder.create()
            .texOffs(0,0).addBox(-8,-8,-10,16,11,18)
            .texOffs(0,98).addBox(-8,3,-10,16,6,14)
            .texOffs(76,64).addBox(-7,-11,-9,14,3,12),PartPose.offset(0,7,-7));
        // Planes start at the belly edge; a tiny front bias separates the beard's rear face.
        var fringe=CubeListBuilder.create().texOffs(76,104).addBox(-8,9,-10.025F,16,3,0);
        for(int side:new int[]{-8,8})fringe.texOffs(76,80).addBox(side,9,-10,0,3,14);
        shoulder.addOrReplaceChild("mane_fringe",fringe,PartPose.ZERO);
        var rump=root.addOrReplaceChild("rump",CubeListBuilder.create().texOffs(0,40).addBox(-7,-4,0,14,13.975F,22),PartPose.offset(0,7,-4));
        rump.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(24,82).addBox(-1,0,0,2,10,2),PartPose.offset(0,0,22));
        var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(72,0).addBox(-6,-4,-7,12,13,12)
            .texOffs(72,32).addBox(-5,3,-10,10,5,4),PartPose.offset(0,9,-19));
        head.addOrReplaceChild("beard",CubeListBuilder.create().texOffs(96,32).addBox(-3,8,-4,6,6,6),PartPose.ZERO);
        for(int sign:new int[]{-1,1}){
            head.addOrReplaceChild("horn"+sign,CubeListBuilder.create().texOffs(40,82).addBox(-1,-5,-1,2,5,2),PartPose.offsetAndRotation(sign*6,-2,-2,0,0,sign*.4F));
            head.addOrReplaceChild("ear"+sign,CubeListBuilder.create().texOffs(56,82).addBox(sign>0?0:-4,-1,-1,4,2,2),PartPose.offset(sign*6,0,1));
        }
        for(String name:new String[]{"front_left","front_right","back_left","back_right"}){
            root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,82).addBox(-2.5F,0,-2.5F,5,8,5),PartPose.offset(name.endsWith("left")?5.475F:-5.475F,16,name.startsWith("front")?-10:12));
        }
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(BisonRenderer.State state){
        super.setupAnim(state);head.yRot=state.yRot*Mth.DEG_TO_RAD;head.xRot=state.xRot*Mth.DEG_TO_RAD;
        head.getChild("horn-1").visible=head.getChild("horn1").visible=head.getChild("beard").visible=!state.isBaby;
        float stride=Mth.cos(state.walkAnimationPos*.6F)*state.walkAnimationSpeed*.7F;
        frontLeft.xRot=backRight.xRot=stride;frontRight.xRot=backLeft.xRot=-stride;
        // The native melee goal's synchronized flag supplies the cue without
        // changing combat. Aim the adult's horns forward, clear of the ground.
        if(state.threatening&&!state.isBaby&&!state.isInWater&&state.activity==0)head.xRot=.6F;
        if(state.activity==1){
            // Tip the muzzle toward grass before lowering it: a shallow nod would
            // leave the mouth hovering while pushing the long beard into the soil.
            head.y+=2;head.xRot=.9F+.02F*Mth.sin(state.ageInTicks*.2F);
        }
        // The mane fringe meets the floor while the solid shoulder stays above it.
        if(state.activity==2){shoulder.y+=5;rump.y+=7;head.y+=1;frontLeft.y+=2.75F;frontRight.y+=2.75F;backLeft.y+=2.75F;backRight.y+=2.75F;frontLeft.xRot=frontRight.xRot=-1.2F;backLeft.xRot=backRight.xRot=1.2F;}
    }
}
