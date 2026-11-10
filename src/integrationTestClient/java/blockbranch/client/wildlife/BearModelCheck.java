package blockbranch.client.wildlife;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
final class BearModelCheck {
    static void verifyRevision(){
        var root=BearModel.layer().bakeRoot();var model=new BearModel(root);var state=new BearRenderer.State();model.setupAnim(state);
        if(!(root.getChild("hump").z-10>root.getChild("body").z-12&&root.getChild("front_left").x+2.5F<7&&root.getChild("front_right").x-2.5F> -7))throw new AssertionError("Bear overlapping external planes remain");
        state.activity=2;model.setupAnim(state);
        if(root.getChild("body").y!=17||root.getChild("hump").y!=10||root.getChild("head").y!=16||root.getChild("front_left").y!=19.25F||root.getChild("back_left").y!=19.25F)throw new AssertionError("Accepted bear sitting translations changed");
        if(Math.abs(root.getChild("front_left").xRot+1.3F)>1e-6||Math.abs(root.getChild("back_left").xRot-1.3F)>1e-6)throw new AssertionError("Accepted bear sitting rotations changed");
        blockbranch.Blockbranch.LOGGER.info("BEAR_GEOMETRY_REGRESSION PASS sittingUnchanged=true humpAndLegPlanesSeparated=true biasModelUnits=0.025");
    }
}
