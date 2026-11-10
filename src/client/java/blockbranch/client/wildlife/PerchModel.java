package blockbranch.client.wildlife;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
/** Simple fish body and planar fins; perch bars and red fins are painted. */
final class PerchModel extends EntityModel<LivingEntityRenderState> {
    private final ModelPart tail,left,right;
    PerchModel(ModelPart root){super(root);tail=root.getChild("tail");left=root.getChild("left_fin");right=root.getChild("right_fin");}
    static LayerDefinition layer(){var mesh=new MeshDefinition();var root=mesh.getRoot();
        root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(0,0).addBox(-1,-1.5F,-3,2,3,6),PartPose.offset(0,21.5F,0));
        root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(20,0).addBox(-1,-1.5F,-2,2,3,2),PartPose.offset(0,21.5F,-3));
        root.addOrReplaceChild("tail",CubeListBuilder.create().texOffs(0,16).addBox(0,-2,0,0,4,3),PartPose.offset(0,21.5F,3));
        root.addOrReplaceChild("dorsal",CubeListBuilder.create().texOffs(10,16).addBox(0,-2,0,0,2,4),PartPose.offset(0,20,-2));
        root.addOrReplaceChild("ventral",CubeListBuilder.create().texOffs(20,16).addBox(0,0,0,0,1,3),PartPose.offset(0,23,0));
        root.addOrReplaceChild("left_fin",CubeListBuilder.create().texOffs(28,16).addBox(0,0,0,1,0,2),PartPose.offset(1,22,-1));
        root.addOrReplaceChild("right_fin",CubeListBuilder.create().texOffs(28,16).addBox(-1,0,0,1,0,2),PartPose.offset(-1,22,-1));
        return LayerDefinition.create(mesh,64,32);
    }
    @Override public void setupAnim(LivingEntityRenderState state){super.setupAnim(state);float pace=state.isInWater?1:1.5F;tail.yRot=-pace*.45F*Mth.sin(state.ageInTicks*.6F);left.yRot=.25F+.15F*Mth.sin(state.ageInTicks*.4F);right.yRot=-left.yRot;
        if(landPose(state)){
            // The land roll puts positive model X below the body. Fold the
            // pectorals inside its width and sweep the tail upward from contact,
            // retaining its period with a lower, ground-bounded maximum lift.
            left.yRot=-Mth.HALF_PI;right.yRot=Mth.HALF_PI;
            left.y+=.025F; // Separate the two folded planes inside the torso.
            tail.yRot=-.175F*(1+Mth.sin(state.ageInTicks*.6F));
        }
    }
    static boolean landPose(LivingEntityRenderState state){return !state.isInWater&&state.deathTime==0&&!state.isUpsideDown&&!state.isAutoSpinAttack;}
}
