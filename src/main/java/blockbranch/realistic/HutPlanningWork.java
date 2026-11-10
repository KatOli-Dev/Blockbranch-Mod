package blockbranch.realistic;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

/** One optional helper for all concurrent world initializations, with no task queue.
 * The caller participates and joins before any complete catalog can be published. */
final class HutPlanningWork {
    private static final Semaphore HELPER=new Semaphore(1);
    @FunctionalInterface interface Range<T> {List<T> compute(int first,int end,BooleanSupplier cancelled);}

    static <T> List<T> run(int size,Range<T> work) {
        checkCancelled(Thread.currentThread()::isInterrupted);
        if(size<2||Runtime.getRuntime().availableProcessors()<2||!HELPER.tryAcquire())
            return work.compute(0,size,Thread.currentThread()::isInterrupted);
        var cancelled=new AtomicBoolean();var failure=new AtomicReference<Throwable>();
        BooleanSupplier stopped=()->cancelled.get()||Thread.currentThread().isInterrupted();
        int middle=size/2;
        Thread helper=null;
        try {
            var task=new FutureTask<List<T>>(() -> {
                try{return work.compute(0,middle,stopped);}
                catch(Throwable t){failure.compareAndSet(null,t);cancelled.set(true);throw propagate(t);}
            });
            helper=Thread.ofPlatform().daemon(true).name("Blockbranch hut planner").unstarted(task);
            helper.start();
            var last=work.compute(middle,size,stopped);
            var first=task.get();checkCancelled(stopped);
            var result=new ArrayList<T>(first.size()+last.size());result.addAll(first);result.addAll(last);
            return result;
        }catch(InterruptedException t){
            Thread.currentThread().interrupt();throw new CancellationException("Hut planning interrupted before publication");
        }catch(ExecutionException t){throw propagate(t.getCause());}
        catch(Throwable t){throw propagate(failure.get()==null?t:failure.get());}
        finally {
            cancelled.set(true);if(helper!=null)helper.interrupt();
            // No abandoned worker can touch planning inputs after a failure or
            // interrupted load. Join even when the caller's interrupt flag is set.
            boolean interrupted=false;
            while(helper!=null&&helper.isAlive())try{helper.join();}catch(InterruptedException t){interrupted=true;}
            HELPER.release();if(interrupted)Thread.currentThread().interrupt();
        }
    }
    static void checkCancelled(BooleanSupplier cancelled){if(cancelled.getAsBoolean())throw new CancellationException("Hut planning cancelled before publication");}
    private static RuntimeException propagate(Throwable t){if(t instanceof Error e)throw e;return t instanceof RuntimeException r?r:new IllegalStateException("Hut planning failed",t);}
    private HutPlanningWork(){}
}
