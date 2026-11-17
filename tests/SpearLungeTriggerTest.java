import com.visionmod.SpearLungeTrigger;

public final class SpearLungeTriggerTest {
    public static void main(String[] args) {
        var trigger = new SpearLungeTrigger();
        assert !trigger.sample(1.2, true) : "Running without a spear action must not emit a lunge";
        trigger.arm(.1);
        assert !trigger.sample(.28, true) : "Walking acceleration is not a lunge";
        assert trigger.sample(.8, true) : "Vanilla spear impulse must emit an effect";
        assert !trigger.sample(1.4, true) : "One action must emit only once";
        trigger.arm(.1);
        for (int i=0;i<10;i++) assert !trigger.sample(.1, true);
        assert !trigger.sample(1.5, true) : "A later impulse must not replay an expired action";
        trigger.arm(.1);
        assert !trigger.sample(.8, false);
        assert !trigger.sample(1.5, true) : "Disabling or changing weapons must clear the pending action";
        trigger.arm(.1); trigger.reset();
        assert !trigger.sample(.8, true) : "World changes must clear pending actions";
        trigger.arm(Double.NaN);
        assert !trigger.sample(.8, true);
        trigger.arm(.1);
        assert !trigger.sample(Double.NaN, true);
        System.out.println("[VisionMod][SPEAR_TRIGGER] PASS: actual impulses, walking, one-shot, expiry, equipment/OFF/world reset, invalid input");
    }
}
