package blockbranch.client.cubic;
import blockbranch.cubic.TerrainWindowPacket;
import java.nio.file.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
/** Real TCP capability mismatch; production JOIN guards perform all rejection. */
public final class ColumnProtocolAudit {
 private static int statePayloads;
 public static void receivedState(){if(Boolean.getBoolean("blockbranch.columnProtocolAudit"))statePayloads++;}
 public static void register(){
  boolean missingWindow=Boolean.getBoolean("blockbranch.columnProtocolMissingWindow");
  net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STARTED.register(client->{if(missingWindow&&ClientPlayNetworking.unregisterGlobalReceiver(TerrainWindowPacket.TYPE.id())==null)throw new AssertionError("Window receiver was not registered before QA removal");});
  long deadline=System.nanoTime()+90_000_000_000L;boolean[] done={false};
  ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{
   if(done[0])return;done[0]=true;var details=handler.getConnection().getDisconnectionDetails();String reason=details==null?"<none>":details.reason().getString();
   String expected=System.getProperty("blockbranch.columnProtocolExpectedReason");
   boolean acceptedReason=expected!=null?reason.equals(expected):missingWindow
     ?reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 15 / metallurgy catalog 20).")
     :(reason.equals("This server requires the same Blockbranch version (column protocol 70 / terrain profile 70).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 70).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 70).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 70 / canopy catalog 169).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 70 / canopy catalog 169).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 72 / canopy catalog 169 / boreal surface catalog 16).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 72 / canopy catalog 169 / boreal surface catalog 16).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 3).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 3).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 3 / metallurgy catalog 2).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 3 / metallurgy catalog 3).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 4 / metallurgy catalog 4).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 5 / metallurgy catalog 5).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 3 / metallurgy catalog 2).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 3 / metallurgy catalog 3).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 4 / metallurgy catalog 4).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 5 / metallurgy catalog 5).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 6 / metallurgy catalog 6).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 6 / metallurgy catalog 6).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 7 / metallurgy catalog 7).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 7 / metallurgy catalog 7).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 7 / metallurgy catalog 8).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 7 / metallurgy catalog 8).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 8 / metallurgy catalog 9).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 8 / metallurgy catalog 9).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 9 / metallurgy catalog 10).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 9 / metallurgy catalog 10).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 10 / metallurgy catalog 11).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 10 / metallurgy catalog 11).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 10 / metallurgy catalog 12).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 10 / metallurgy catalog 12).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 10 / metallurgy catalog 13).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 10 / metallurgy catalog 13).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 10 / metallurgy catalog 14).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 10 / metallurgy catalog 14).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 11 / metallurgy catalog 15).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 11 / metallurgy catalog 15).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 11 / metallurgy catalog 16).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 11 / metallurgy catalog 16).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 12 / metallurgy catalog 17).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 12 / metallurgy catalog 17).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 13 / metallurgy catalog 18).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 13 / metallurgy catalog 18).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 14 / metallurgy catalog 19).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 14 / metallurgy catalog 19).")
       ||reason.equals("This server requires the same Blockbranch version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 15 / metallurgy catalog 20).")
       ||reason.equals("This client requires the same Blockbranch server version (column protocol 70 / window protocol 71 / terrain profile 74 / canopy catalog 169 / boreal surface catalog 16 / short reed stacks / ore definition 15 / metallurgy catalog 20)."));
   boolean pass=!handler.getConnection().isMemoryConnection()&&acceptedReason&&statePayloads==0;
   write((pass?"PASS":"FAIL")+" real TCP statePayloads="+statePayloads+" "+(missingWindow?"server rejects missing window capability":"mixed catalog rejection")+": "+reason);client.stop();
  });
  ClientLoginConnectionEvents.DISCONNECT.register((handler,client)->{if(!done[0]){done[0]=true;write("FAIL disconnected during login before production play capability guard");client.stop();}});
  ClientTickEvents.END_CLIENT_TICK.register(client->{if(!done[0]&&System.nanoTime()>deadline){done[0]=true;write("FAIL protocol rejection deadline");client.stop();}});
 }
 private static void write(String s){try{Files.writeString(Path.of(System.getProperty("blockbranch.columnProtocolResult")),s+"\n");}catch(Exception e){throw new RuntimeException(e);}}
 private ColumnProtocolAudit(){}
}
