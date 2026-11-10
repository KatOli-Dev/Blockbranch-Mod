package blockbranch.realistic;
/** Executes the production admission arithmetic; does not simulate chunk generation or claim multi-client coverage. */
public final class ProgressiveMapPolicyQualification {
 public static void check(){
  try{
   var f=ProgressiveMapDrawing.class.getDeclaredField("TICKET");f.setAccessible(true);var own=(net.minecraft.server.level.TicketType)f.get(null);
   WellInteraction.require(own!=net.minecraft.server.level.TicketType.PLAYER_LOADING&&!own.persist()&&!own.doesSimulate(),"Distinct nonpersisted, nonsimulating map ticket identity");
   var storage=new net.minecraft.world.level.TicketStorage();var pos=new net.minecraft.world.level.ChunkPos(0,0);
   storage.addTicket(new net.minecraft.server.level.Ticket(net.minecraft.server.level.TicketType.PLAYER_LOADING,33),pos);storage.addTicket(new net.minecraft.server.level.Ticket(own,33),pos);
   storage.removeTicketWithRadius(own,pos,0);
   WellInteraction.require(storage.getTickets(pos.pack()).size()==1&&storage.getTickets(pos.pack()).getFirst().getType()==net.minecraft.server.level.TicketType.PLAYER_LOADING,"Actual native removal preserves another owner at same coordinate/level");
  }catch(ReflectiveOperationException e){throw new AssertionError(e);}

  for(int count=1;count<=256;count++){
   boolean[][] seen=new boolean[count][2];
   for(long tick=1;tick<=count*8L;tick++){
    var unique=new java.util.HashSet<Integer>();
    for(int slot=0;slot<Math.min(4,count);slot++){
     int player=ProgressiveMapDrawing.grantIndex(tick,slot,count);
     WellInteraction.require(player>=0&&player<count&&unique.add(player),"Four distinct admitted players per tick");
     seen[player][ProgressiveMapDrawing.offhandTurn(tick,count)?1:0]=true;
    }
   }
   for(var hands:seen)WellInteraction.require(hands[0]&&hands[1],"Both held maps eventually admitted for every player");
  }
 } public static void checkQueues(net.minecraft.server.level.ServerLevel level)throws Exception{
  var player=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(),"QueueFixture"),net.minecraft.server.level.ClientInformation.createDefault());
  var data=net.minecraft.world.level.saveddata.maps.MapItemSavedData.createFresh(0,0,(byte)1,true,true,level.dimension());
  Class<?> jobType=Class.forName("blockbranch.realistic.ProgressiveMapDrawing$Job"),stateType=Class.forName("blockbranch.realistic.ProgressiveMapDrawing$State");
  var jc=jobType.getDeclaredConstructors()[0];jc.setAccessible(true);
  Object a=jc.newInstance(level,player.getUUID(),data,data.getHoldingPlayer(player)),b=jc.newInstance(level,java.util.UUID.randomUUID(),data,data.getHoldingPlayer(player));
  var sc=stateType.getDeclaredConstructor();sc.setAccessible(true);Object state=sc.newInstance();
  var replacement=new net.minecraft.server.level.ServerPlayer(level.getServer(),level,new com.mojang.authlib.GameProfile(player.getUUID(),"QueueReplacement"),net.minecraft.server.level.ClientInformation.createDefault());
  var firstHolding=data.getHoldingPlayer(player);var nextHolding=data.getHoldingPlayer(replacement);WellInteraction.require(firstHolding!=nextHolding,"Native holding identity differs across same-UUID player instances");
  var jobs=stateType.getDeclaredField("jobs");jobs.setAccessible(true);((java.util.List<Object>)jobs.get(state)).add(a);
  var find=stateType.getDeclaredMethod("find",net.minecraft.server.level.ServerLevel.class,java.util.UUID.class,net.minecraft.world.level.saveddata.maps.MapItemSavedData.class,net.minecraft.world.level.saveddata.maps.MapItemSavedData.HoldingPlayer.class);find.setAccessible(true);
  WellInteraction.require(find.invoke(state,level,player.getUUID(),data,firstHolding)==a&&find.invoke(state,level,player.getUUID(),data,nextHolding)==null,"Actual cache lookup rejects prior connection's holding node");
  var pending=stateType.getDeclaredField("pending");pending.setAccessible(true);pending.set(state,a);
  var cc=ProgressiveMapDrawing.Context.class.getDeclaredConstructors()[0];cc.setAccessible(true);
  WellInteraction.require(level.getChunkSource().getChunkNow(100000,100000)==null,"Queue test has no loaded chunk; test will not generate it");
  try(var context=(ProgressiveMapDrawing.Context)cc.newInstance(state,b,true)){try{context.chunk(100000,100000,0);throw new AssertionError("Other owner must defer");}catch(ProgressiveMapDrawing.Yield expected){}}
  var release=stateType.getDeclaredMethod("release",jobType);release.setAccessible(true);release.invoke(state,a);
  try(var context=(ProgressiveMapDrawing.Context)cc.newInstance(state,a,true)){try{context.chunk(100000,100000,0);throw new AssertionError("Prior owner must not bypass queued waiter");}catch(ProgressiveMapDrawing.Yield expected){}}
  var waiting=stateType.getDeclaredField("waiting");waiting.setAccessible(true);var queue=(java.util.Set<?>)waiting.get(state);
  WellInteraction.require(pending.get(state)==null&&queue.size()==2&&queue.iterator().next()==b,"Actual production guard preserves fair waiting order without starting generation");
  release.invoke(state,b);WellInteraction.require(queue.size()==1&&queue.iterator().next()==a,"Cancelling waiter wakes next owner");release.invoke(state,a);
  WellInteraction.require(queue.isEmpty()&&ProgressiveMapDrawing.current()==null,"Queue fixture releases all state/context");
  var flight=stateType.getDeclaredField("flight");flight.setAccessible(true);var unfinished=new java.util.concurrent.CompletableFuture<>();flight.set(state,unfinished);pending.set(state,a);
  var fp=stateType.getDeclaredField("flightPos");fp.setAccessible(true);var fl=stateType.getDeclaredField("flightLevel");fl.setAccessible(true);
  var target=new net.minecraft.world.level.ChunkPos(100000,100000);fp.set(state,target);fl.set(state,level);
  var typeField=ProgressiveMapDrawing.class.getDeclaredField("TICKET");typeField.setAccessible(true);var type=(net.minecraft.server.level.TicketType)typeField.get(null);
  var tickets=jobType.getDeclaredField("tickets");tickets.setAccessible(true);((java.util.Set<net.minecraft.world.level.ChunkPos>)tickets.get(a)).add(target);level.getChunkSource().addTicketWithRadius(type,target,0);
  var storageField=net.minecraft.server.level.ServerChunkCache.class.getDeclaredField("ticketStorage");storageField.setAccessible(true);var storage=(net.minecraft.world.level.TicketStorage)storageField.get(level.getChunkSource());
  release.invoke(state,a); // cancellation drops column ownership, retaining only the dispatched target
  WellInteraction.require(storage.getTickets(target.pack()).stream().anyMatch(t->t.getType()==type),"Cancelled native request retains its single draining ticket");
  var canRequest=stateType.getDeclaredMethod("canRequest");canRequest.setAccessible(true);
  WellInteraction.require(!(Boolean)canRequest.invoke(state),"Cancelled but unfinished generation retains its credit");
  try(var context=(ProgressiveMapDrawing.Context)cc.newInstance(state,b,true)){try{context.chunk(100000,100000,0);throw new AssertionError("Draining work must block another generation submission");}catch(ProgressiveMapDrawing.Yield expected){}}
  WellInteraction.require(pending.get(state)==null&&flight.get(state)==unfinished,"Actual request guard cannot replace unfinished retired work");
  unfinished.complete(null);WellInteraction.require((Boolean)canRequest.invoke(state),"Native completion returns the generation credit");WellInteraction.require(storage.getTickets(target.pack()).stream().noneMatch(t->t.getType()==type),"Completion between ticks clears the retired ticket before a new owner can replace its metadata");release.invoke(state,b);
  var clearFlight=stateType.getDeclaredMethod("clearFlight");clearFlight.setAccessible(true);clearFlight.invoke(state);
  WellInteraction.require(storage.getTickets(target.pack()).stream().noneMatch(t->t.getType()==type),"Native completion releases the draining ticket");
  WellInteraction.require(queue.isEmpty()&&ProgressiveMapDrawing.current()==null,"Draining fixture cleans up");
  var reserveSlot=stateType.getDeclaredMethod("reserveSlot",int.class);reserveSlot.setAccessible(true);
  var descriptorJobs=(java.util.List<Object>)jobs.get(state);descriptorJobs.clear();
  var descriptorQueue=(java.util.Set<Object>)waiting.get(state);descriptorQueue.clear();
  var holding=data.getHoldingPlayer(player);Object first=null;
  // A stable 256-player server can hold two maps each. Exercise production eviction,
  // with no chunks requested, rather than duplicating its policy in a simulation.
  for(int n=0;n<512;n++){
   reserveSlot.invoke(state,256);
   Object descriptor=jc.newInstance(level,java.util.UUID.randomUUID(),data,holding);
   descriptorJobs.add(descriptor);descriptorQueue.add(descriptor);if(first==null)first=descriptor;
  }
  WellInteraction.require(descriptorJobs.size()==512&&descriptorQueue.size()==512&&descriptorQueue.iterator().next()==first,"Two held maps per participant cannot churn away the FIFO head");
  for(int n=512;n<ProgressiveMapDrawing.descriptorLimit(256);n++){
   reserveSlot.invoke(state,256);Object descriptor=jc.newInstance(level,java.util.UUID.randomUUID(),data,holding);descriptorJobs.add(descriptor);descriptorQueue.add(descriptor);
  }
  int limit=descriptorJobs.size();reserveSlot.invoke(state,256);
  WellInteraction.require(descriptorJobs.size()==limit-1&&descriptorQueue.size()==limit-1&&!descriptorJobs.contains(first),"Abnormal extra calls still obey the explicit descriptor bound");
  WellInteraction.require(ProgressiveMapDrawing.descriptorLimit(1)==64&&ProgressiveMapDrawing.descriptorLimit(256)==1536,"Descriptor budget covers two hands through the three-tick cleanup window");
  descriptorJobs.clear();descriptorQueue.clear();
 }

}
