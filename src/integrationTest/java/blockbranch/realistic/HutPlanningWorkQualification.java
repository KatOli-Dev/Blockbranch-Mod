package blockbranch.realistic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Lifecycle checks independent of Minecraft: no pending results survive return. */
public final class HutPlanningWorkQualification {
    public static void main(String[] args) throws Exception {
        var ordered=HutPlanningWork.run(100,(a,b,c)->{var out=new ArrayList<Integer>();for(int n=a;n<b;n++)out.add(n);return out;});
        require(ordered.equals(java.util.stream.IntStream.range(0,100).boxed().toList()),"range order");
        var helperEntered=new CountDownLatch(1);var ownerEntered=new CountDownLatch(1);var release=new CountDownLatch(1);
        var error=new AtomicReference<Throwable>();
        var owner=Thread.ofPlatform().daemon(true).start(()->{try{HutPlanningWork.run(2,(a,b,c)->{
            if(a==0)helperEntered.countDown();else ownerEntered.countDown();
            while(release.getCount()!=0){HutPlanningWork.checkCancelled(c);Thread.onSpinWait();}
            return List.of(a);
        });}catch(Throwable t){error.set(t);}});
        require(helperEntered.await(5,TimeUnit.SECONDS)&&ownerEntered.await(5,TimeUnit.SECONDS),"helper admitted");
        Thread caller=Thread.currentThread();
        var fallback=HutPlanningWork.run(2,(a,b,c)->{require(Thread.currentThread()==caller&&a==0&&b==2,"saturation is synchronous whole-range fallback");return List.of(7);});
        require(fallback.equals(List.of(7)),"fallback complete");release.countDown();owner.join(5000);require(!owner.isAlive()&&error.get()==null,"joined success");
        var expected=new IllegalArgumentException("worker sentinel");
        try{HutPlanningWork.run(2,(a,b,c)->{if(a==0)throw expected;while(true)HutPlanningWork.checkCancelled(c);});throw new AssertionError("failure hidden");}
        catch(IllegalArgumentException actual){require(actual==expected,"original worker failure");}
        final var ownerFailure=new IllegalArgumentException("owner sentinel");
        try{HutPlanningWork.run(2,(a,b,c)->{if(a==1)throw ownerFailure;while(true)HutPlanningWork.checkCancelled(c);});throw new AssertionError("failure hidden");}
        catch(IllegalArgumentException actual){require(actual==ownerFailure,"original owner failure");}
        var entered=new CountDownLatch(2);var interrupted=new AtomicReference<Boolean>();error.set(null);
        var interruptOwner=Thread.ofPlatform().daemon(true).start(()->{try{HutPlanningWork.run(2,(a,b,c)->{entered.countDown();while(true)HutPlanningWork.checkCancelled(c);});}
            catch(Throwable t){error.set(t);interrupted.set(Thread.currentThread().isInterrupted());}});
        require(entered.await(5,TimeUnit.SECONDS),"both cancellation ranges started");interruptOwner.interrupt();interruptOwner.join(5000);
        require(!interruptOwner.isAlive()&&error.get() instanceof CancellationException&&Boolean.TRUE.equals(interrupted.get()),"interrupt retained and worker joined");
        require(Thread.getAllStackTraces().keySet().stream().noneMatch(t->t.getName().equals("Blockbranch hut planner")&&t.isAlive()),"no leaked helper");
        require(HutPlanningWork.run(2,(a,b,c)->List.of(a)).equals(List.of(0,1)),"permit recovered after failure/interrupt");
        Thread.currentThread().interrupt();try{HutPlanningWork.run(2,(a,b,c)->{throw new AssertionError("preinterrupted work admitted");});throw new AssertionError("preinterrupt hidden");}
        catch(CancellationException good){require(Thread.currentThread().isInterrupted(),"preinterrupt retained");}finally{Thread.interrupted();}
        System.out.println("PASS deterministic order, saturated no-queue fallback, original worker/owner failures, cancellation/join, interrupt preservation and permit recovery");
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
