package net.minecraft.client;
/** Unit-test scheduler only; does not emulate Minecraft rendering or API compatibility. */
public final class Minecraft {
    private static final Minecraft INSTANCE=new Minecraft();
    public static Minecraft getInstance(){return INSTANCE;}
    public void execute(Runnable task){task.run();}
}
