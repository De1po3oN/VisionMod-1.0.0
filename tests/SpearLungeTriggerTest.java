import com.visionmod.SpearLungeTrigger;

public final class SpearLungeTriggerTest {
    public static void main(String[] args) {
        var trigger = new SpearLungeTrigger();
        assert trigger.finish(1.2,0,true)==null : "Walking must not arm a lunge";
        trigger.arm(.2,0);
        assert trigger.finish(.2,0,true)==null : "An unsuccessful vanilla lunge must not emit";
        assert trigger.finish(1.2,0,true)==null : "A later impulse must not replay a failed action";
        trigger.arm(.1,0);
        var forward=trigger.finish(.8,0,true);
        assert forward!=null && Math.abs(forward.x()-.7)<1e-6;
        assert trigger.finish(1.4,0,true)==null : "One native action must emit only once";
        trigger.arm(-.5,0);
        var backwards=trigger.finish(.1,0,true);
        assert backwards!=null && backwards.x()>0 : "A lunge while moving backwards must use the impulse direction";
        trigger.arm(0,0);
        assert trigger.finish(0,.15,true)!=null : "Small valid lunge impulses must be visible";
        trigger.arm(.1,0);
        assert trigger.finish(.8,0,false)==null;
        assert trigger.finish(1.5,0,true)==null : "OFF must consume pending actions";
        trigger.arm(.1,0); trigger.reset();
        assert trigger.finish(.8,0,true)==null : "World changes must clear pending actions";
        trigger.arm(Double.NaN,0);
        assert trigger.finish(.8,0,true)==null;
        trigger.arm(.1,0);
        assert trigger.finish(Double.NaN,0,true)==null;
        System.out.println("[VisionMod][SPEAR_TRIGGER] PASS: native impulses, backwards motion, failed actions, one-shot, equipment/OFF/world reset, invalid input");
    }
}
