package com.visionmod;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;
import static com.visionmod.GuiTestInput.*;

/** Navigation is changed with real clicks/typing and reopened through the actual menu key. */
final class VisionMenuMemoryGameTest {
    static void run(ClientGameTestContext context)throws java.io.IOException{
        context.getInput().resizeWindow(640,480);context.waitTicks(2);
        open(context);
        VisionDashboardScreen dashboard=context.computeOnClient(mc->(VisionDashboardScreen)mc.screen);
        DashboardLayout layout=context.computeOnClient(mc->DashboardLayout.of(mc.screen.width,mc.screen.height));
        int left=context.computeOnClient(mc->(mc.screen.width-layout.width())/2),top=context.computeOnClient(mc->(mc.screen.height-layout.height())/2);
        int content=left+layout.contentLeft();
        click(context,left+24,top+layout.tabTop(5)+layout.tabHeight()/2,0);
        next(context,layout,content,top);
        assertView(context,dashboard,5,1,"");
        close(context);open(context);assertView(context,dashboard,5,1,"");
        click(context,content+40,top+layout.searchTop()+12,0);context.getInput().typeChars("small");context.waitTicks(1);
        next(context,layout,content,top);assertView(context,dashboard,5,1,"small");
        close(context);open(context);assertView(context,dashboard,5,1,"small");
        // Details and nested settings also survive closing straight to the world.
        click(context,left+layout.cardLeft(0)+25,top+layout.cardTop(0)+20,1);
        Object details=context.computeOnClient(mc->field(dashboard,"details"));check(details!=null,"Fixture did not open module details");
        close(context);open(context);context.runOnClient(mc->check(field(dashboard,"details")==details,"Module details were lost"));
        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);
        click(context,left+layout.themeLeft()+layout.themeWidth()/2,top+layout.navigationTop()+layout.footerHeight()/2,0);
        VisionAppearanceScreen appearance=context.computeOnClient(mc->(VisionAppearanceScreen)mc.screen);
        PanelLayout panel=context.computeOnClient(mc->PanelLayout.of(mc.screen.width,mc.screen.height));
        click(context,panel.x()+66,panel.y()+panel.height()-19,0);
        context.runOnClient(mc->check((int)field(appearance,"page")==1,"Fixture did not open the second settings page"));
        close(context);open(context);context.runOnClient(mc->check(mc.screen==appearance&&(int)field(appearance,"page")==1,"Nested settings screen/page was lost"));
        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);assertView(context,dashboard,5,1,"small");
        context.getInput().resizeWindow(680,480);context.waitTicks(2);assertView(context,dashboard,5,1,"small");
        VisualWorldGameTest.capture(context,"11-menu-session-memory");
        context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);context.waitFor(mc->mc.screen==null,40);
        open(context);assertView(context,dashboard,5,1,"small");close(context);
        context.getInput().resizeWindow(854,480);
        System.out.println("[VisionMod][MENU_MEMORY] PASS: category, page, search, module details, nested settings/page, parent return, resize, Escape and menu-key reopen without double toggle");
    }
    private static void next(ClientGameTestContext context,DashboardLayout layout,int content,int top){
        click(context,content+45,top+layout.navigationTop()+layout.footerHeight()/2,0);
    }
    private static void open(ClientGameTestContext context){context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);context.waitTicks(2);}
    private static void close(ClientGameTestContext context){
        context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);context.waitTicks(2);
        context.runOnClient(mc->check(mc.screen==null,"Menu key failed to close or reopened itself"));
    }
    private static void assertView(ClientGameTestContext context,VisionDashboardScreen expected,int category,int page,String query){
        context.runOnClient(mc->{
            check(mc.screen==expected,"Reopening replaced the previous dashboard");
            check((int)field(expected,"category")==category&&(int)field(expected,"page")==page&&((EditBox)field(expected,"search")).getValue().equals(query),"Dashboard lost category/page/query");
        });
    }
}
