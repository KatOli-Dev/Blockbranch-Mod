package blockbranch.client.wildlife;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import org.joml.Vector3f;

/** Native baked bounds, not perspective estimates. Called only by the opt-in review. */
final class WildlifeModelGroundAudit {
    static void verify(){
        for(boolean baby:new boolean[]{false,true}){
            var boar=BoarModel.layer().bakeRoot();var bs=new BoarRenderer.State();bs.isBaby=baby;bs.activity=2;new BoarModel(boar).setupAnim(bs);ground("boar",boar,baby);
            var deer=DeerModel.layer().bakeRoot();var ds=new DeerRenderer.State();ds.isBaby=baby;ds.activity=2;new DeerModel(deer).setupAnim(ds);ground("deer",deer,baby);
            var bison=BisonModel.layer().bakeRoot();var bis=new BisonRenderer.State();bis.isBaby=baby;bis.activity=2;new BisonModel(bison).setupAnim(bis);ground("bison",bison,baby);
            var bear=BearModel.layer().bakeRoot();var brs=new BearRenderer.State();brs.isBaby=baby;brs.activity=2;new BearModel(bear).setupAnim(brs);ground("bear",bear,baby);
            var beaver=BeaverModel.layer().bakeRoot();var bvs=new BeaverRenderer.State();bvs.isBaby=baby;bvs.activity=2;new BeaverModel(beaver).setupAnim(bvs);ground("beaver",beaver,baby);
            var mallard=MallardModel.layer().bakeRoot();var ms=new MallardRenderer.State();ms.isBaby=baby;ms.activity=2;new MallardModel(mallard).setupAnim(ms);ground("mallard",mallard,baby);
            var seal=SealModel.layer().bakeRoot();var ss=new SealRenderer.State();ss.isBaby=baby;ss.activity=2;new SealModel(seal).setupAnim(ss);ground("seal",seal,baby);
        }
        // Perch has no sitting/rest pose; its planar fins are intentional, not intersecting fur boxes.
        var perch=PerchModel.layer().bakeRoot();var ps=new net.minecraft.client.renderer.entity.state.LivingEntityRenderState();ps.isInWater=true;new PerchModel(perch).setupAnim(ps);
        blockbranch.Blockbranch.LOGGER.info("WILDLIFE_MODEL_AUDIT PASS seven grounded rest poses adult/baby; perch swimming geometry baked");
    }
    private static void ground(String species,ModelPart root,boolean baby){
        float age=baby?.5F:1;var pose=new PoseStack();pose.scale(age,age,age);float[] bottom={Float.NEGATIVE_INFINITY};
        root.visit(pose,(matrix,path,index,cube)->{
            for(float x:new float[]{cube.minX,cube.maxX})for(float y:new float[]{cube.minY,cube.maxY})for(float z:new float[]{cube.minZ,cube.maxZ}){
                var point=matrix.pose().transformPosition(new Vector3f(x/16,y/16,z/16));bottom[0]=Math.max(bottom[0],point.y());
            }
        });
        // Preserve the accepted bear pose's 0.014-model-unit sole rounding; reject visible floating/sinking.
        if(Math.abs(bottom[0]-1.5F*age)>.002F*age)throw new AssertionError(species+" baby="+baby+" resting bottom="+bottom[0]);
    }
    private WildlifeModelGroundAudit(){}
}
