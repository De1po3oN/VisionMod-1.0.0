package com.visionmod;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;
import static com.visionmod.GuiTestInput.*;

/** Real input and framebuffer captures at minimum, ordinary and desktop GUI sizes. */
final class VisionGuiDesignGameTest {
    static void run(ClientGameTestContext context)throws Exception{
        int scale=context.computeOnClient(mc->mc.options.guiScale().get());
        int palette=VisionMenuTheme.palette();
        context.setScreen(()->null);context.runOnClient(mc->mc.options.guiScale().set(2));
        try{
            int[][] sizes={{640,480},{1280,720},{1440,900}};
            String[] names={"31-gui-compact","32-gui-standard","33-gui-desktop"};
            for(int stage=0;stage<sizes.length;stage++){
                context.getInput().resizeWindow(sizes[stage][0],sizes[stage][1]);context.waitTicks(2);
                context.setScreen(VisionDashboardScreen::new);context.waitTicks(3);
                DashboardLayout layout=context.computeOnClient(mc->DashboardLayout.of(mc.screen.width,mc.screen.height));
                int left=context.computeOnClient(mc->(mc.screen.width-layout.width())/2),top=context.computeOnClient(mc->(mc.screen.height-layout.height())/2);
                int minimum=stage==0?2:stage==1?4:6;
                check(layout.cards()>=minimum,"Dashboard wastes space at "+sizes[stage][0]+"x"+sizes[stage][1]);
                boolean before=context.computeOnClient(mc->VisionModClient.fullBright);
                click(context,left+layout.cardLeft(0)+15,top+layout.cardTop(0)+15,0);context.waitTicks(1);
                context.runOnClient(mc->check(VisionModClient.fullBright!=before,"Visible card click missed its toggle"));
                click(context,left+layout.cardLeft(0)+15,top+layout.cardTop(0)+15,0);context.waitTicks(1);
                click(context,left+20,top+layout.tabTop(3)+layout.tabHeight()/2,0);context.waitTicks(1);
                context.runOnClient(mc->check((int)field(mc.screen,"category")==3,"Visible navigation icon missed its category"));
                // The padded search surface is clickable, including outside EditBox's text bounds.
                click(context,left+layout.contentLeft()+3,top+layout.searchTop()+3,0);
                context.getInput().typeChars("free look");context.waitTicks(2);
                context.runOnClient(mc->{
                    check(((EditBox)field(mc.screen,"search")).getValue().equals("free look"),"Search padding did not focus the input");
                    check(((java.util.List<?>)field(mc.screen,"cachedModules")).size()==1,"Typed search did not filter modules");
                });
                click(context,left+layout.contentLeft()+layout.contentWidth()-13,top+layout.searchTop()+12,0);context.waitTicks(1);
                context.runOnClient(mc->check(((EditBox)field(mc.screen,"search")).getValue().isEmpty(),"Search clear button did not clear the query"));
                click(context,left+20,top+layout.tabTop(0)+layout.tabHeight()/2,0);context.waitTicks(5);
                move(context,left+layout.cardLeft(0)+20,top+layout.cardTop(0)+15);
                VisualWorldGameTest.capture(context,names[stage]);
                int gear=layout.dense()?14:18;
                click(context,left+layout.cardLeft(0)+layout.cardWidth()-gear/2-8,
                    top+layout.cardTop(0)+layout.cardHeight()-gear/2-6,0);context.waitTicks(1);
                context.runOnClient(mc->{
                    check(field(mc.screen,"details")!=null,"Visible settings button did not open module details");
                    check(VisionModClient.fullBright==before,"Settings button toggled the module instead of opening details");
                });
                context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);context.waitTicks(1);
            }
            context.setScreen(VisionAppearanceScreen::new);context.waitTicks(3);
            VisualWorldGameTest.capture(context,"34-gui-appearance");
            context.getInput().pressKey(GLFW.GLFW_KEY_ESCAPE);context.waitTicks(2);
            context.runOnClient(mc->VisionMenuTheme.setPalette(2));context.waitTicks(4);
            VisualWorldGameTest.capture(context,"35-gui-mint-theme");
            Object screen=context.computeOnClient(mc->mc.screen);
            context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);context.waitTicks(2);
            context.runOnClient(mc->check(mc.screen==null,"Redesigned GUI did not close with the menu key"));
            context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);context.waitTicks(2);
            context.runOnClient(mc->check(mc.screen==screen,"Redesigned GUI lost its session instance"));
            System.out.println("[VisionMod][GUI_DESIGN] PASS: 2/4/6 visible cards, 640/1280/1440 framebuffers, actual module/category/search-padding/clear clicks, appearance/theme and menu-key session");
        }finally{
            context.setScreen(()->null);context.runOnClient(mc->{mc.options.guiScale().set(scale);VisionMenuTheme.setPalette(palette);});
            context.getInput().resizeWindow(854,480);context.waitTicks(2);
        }
    }
}
