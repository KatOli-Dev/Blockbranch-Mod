package blockbranch.wildlife;
import net.minecraft.server.level.ServerPlayer;
/** Camera movement confined to the opt-in staged review and its loaded neighborhood. */
public final class WildlifeReviewCamera {
    public static void move(ServerPlayer player,WildlifeProbe packet){
        if(packet.stage()!=30||!player.isSpectator())return;
        String[] parts=packet.message().split(" ");if(parts.length!=5)throw new IllegalArgumentException("Review camera pose");double[] v=new double[5];
        for(int i=0;i<5;i++){v[i]=Double.parseDouble(parts[i]);if(!Double.isFinite(v[i]))throw new IllegalArgumentException("Nonfinite camera pose");}
        if(player.position().distanceToSqr(v[0],v[1],v[2])>32*32)throw new IllegalArgumentException("Review camera outside staged neighborhood");
        var server=player.level().getServer();server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"tp "+player.getName().getString()+" "+packet.message());
    }
    private WildlifeReviewCamera(){}
}
