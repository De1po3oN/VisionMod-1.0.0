import com.visionmod.SpearLungeTrigger;

public final class SpearLungeTriggerTest {
    public static void main(String[] args) {
        var trigger=new SpearLungeTrigger();
        assert trigger.sample(1.2,0,true)==null : "Walking must not arm a lunge";
        trigger.arm(0,0,1,0,.2);
        assert trigger.sample(0,0,true)==null : "Vanilla can queue the move until the next tick";
        assert trigger.sample(.2,0,true)==null : "Ordinary walking is not a lunge";
        var forward=trigger.sample(1,0,true);
        assert forward!=null && Math.abs(forward.x()-.8)<1e-6;
        assert trigger.sample(2,0,true)==null : "One native action must emit only once";
        trigger.arm(0,0,1,0,.2);
        assert trigger.sample(-.2,0,true)==null;
        assert trigger.sample(.5,0,true)!=null : "Moving backwards before a lunge must remain supported";
        trigger.arm(0,0,1,0,0);
        assert trigger.sample(.5,0,true)!=null : "Small valid lunge movements must be visible";
        trigger.arm(0,0,1,0,0);
        assert trigger.sample(-1,0,true)==null : "Backward knockback is not a forward lunge";
        assert trigger.sample(-1,1,true)==null : "Lateral knockback is not a forward lunge";
        trigger.arm(0,0,1,0,0);
        for(int i=0;i<16;i++)assert trigger.sample(0,0,true)==null;
        assert trigger.sample(1,0,true)==null : "Failed vanilla actions must expire";
        trigger.arm(0,0,1,0,0);
        assert trigger.sample(20,0,true)==null : "Teleports must not emit";
        trigger.arm(0,0,1,0,0);
        assert trigger.sample(1,0,false)==null;
        assert trigger.sample(2,0,true)==null : "OFF must consume pending actions";
        trigger.arm(0,0,1,0,0);trigger.reset();
        assert trigger.sample(1,0,true)==null : "World changes must clear pending actions";
        trigger.arm(Double.NaN,0,1,0,0);
        assert trigger.sample(1,0,true)==null;
        trigger.arm(0,0,1,0,0);
        assert trigger.sample(Double.NaN,0,true)==null;
        System.out.println("[VisionMod][SPEAR_TRIGGER] PASS: queued vanilla movement, walking, backwards motion, one-shot, knockback direction, expiry, teleport/OFF/world reset");
    }
}
