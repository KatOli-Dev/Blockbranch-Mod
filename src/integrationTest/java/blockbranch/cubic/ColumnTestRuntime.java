package blockbranch.cubic;
public final class ColumnTestRuntime {
    public static void initialize() {
        if(Boolean.getBoolean("blockbranch.columnNetworkSmoke")) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->ColumnNetworkSmoke.joined(handler.player));
            ColumnNetworkSmoke.register();
        }
        if(Boolean.getBoolean("blockbranch.columnSmoke"))ColumnSmoke.register();
    }
}
