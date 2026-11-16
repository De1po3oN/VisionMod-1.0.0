import com.visionmod.SystemMediaReader;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
/** Calls the actual WinRT backend; an empty session is valid, silence is not. */
public class WindowsMediaSmokeTest {
    public static void main(String[] args)throws Exception{
        CountDownLatch received=new CountDownLatch(1);
        AtomicReference<Throwable> failure=new AtomicReference<>();
        try(var reader=new SystemMediaReader()){
            Thread worker=new Thread(()->{
                try{reader.read(track->{received.countDown();reader.close();});}
                catch(Exception e){if(received.getCount()!=0)failure.set(e);}
            });worker.setDaemon(true);worker.start();
            if(!received.await(25,TimeUnit.SECONDS))throw new AssertionError("WinRT reader returned no metadata/empty-session response",failure.get());
            worker.join(5000);
            if(worker.isAlive())throw new AssertionError("Media reader did not stop");
            System.out.println("PASS: Windows media API responded and child process stopped (not a live-song test)");
        }
    }
}
