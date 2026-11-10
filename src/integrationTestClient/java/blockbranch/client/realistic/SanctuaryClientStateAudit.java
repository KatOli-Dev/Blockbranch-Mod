package blockbranch.client.realistic;

import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import blockbranch.realistic.ReimaginedSanctuaryPiece;

/** Read-only decoded-client agreement with the current integrated server's native voxel export. */
final class SanctuaryClientStateAudit {
    private static Map<BlockPos,String> expected;
    static void verify(net.minecraft.client.Minecraft client,ReimaginedSanctuaryPiece piece,Path dir,boolean reopen,int view)throws Exception {
        if(expected==null){
            expected=new HashMap<>();String prefix=Files.exists(dir.resolve("reopen-native-core.voxels.jsonl"))?"reopen-":"";
            for(String file:List.of("native-core.voxels.jsonl","native-foundation.voxels.jsonl"))try(var lines=Files.lines(dir.resolve(prefix+file))){lines.forEach(line->{var row=com.google.gson.JsonParser.parseString(line).getAsJsonObject();var p=piece.local(row.get("x").getAsInt(),row.get("y").getAsInt(),row.get("z").getAsInt());if(expected.put(p,row.get("state").getAsString())!=null)throw new AssertionError("Duplicate native server voxel "+p);});}
            if(expected.size()!=191748)throw new AssertionError("Complete core and foundation server voxel export");
        }
        int mismatch=0;var examples=new ArrayList<Object>();
        for(var entry:expected.entrySet()){
            String state=client.level.getBlockState(entry.getKey()).toString();
            if(!entry.getValue().equals(state)){mismatch++;if(examples.size()<8)examples.add(Map.of("world",entry.getKey().toShortString(),"server",entry.getValue(),"client",state));}
        }
        var result=Map.of("view",view,"comparedVoxels",expected.size(),"mismatches",mismatch,"examples",examples);
        var file=dir.resolve((reopen?"reopen-":"")+"client-state-agreement-"+view+".json");if(Files.exists(file))throw new AssertionError("Preserve prior native client-state witness");
        Files.writeString(file,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(result));
        if(mismatch!=0)throw new AssertionError("Native client/server decoded voxel agreement: "+result);
    }
}
