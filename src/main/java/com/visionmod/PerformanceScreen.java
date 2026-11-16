package com.visionmod;

public final class PerformanceScreen extends VisionOptionsScreen {
    public PerformanceScreen(){super("ПРОИЗВОДИТЕЛЬНОСТЬ");}
    @Override protected void options(){
        String[] names={"MAX FPS","BALANCED","VISUAL"};
        for(int i=0;i<3;i++){final int id=i;option(names[i],()->VisionPerformance.preset,()->VisionHotkeys.applyPreset(id),VisionHotkeys.preset(i));}
        option("Реальный FPS Boost",()->VisionRenderBoost.enabled?"ВКЛ · настройки обратимы":"ВЫКЛ",VisionRenderBoost::toggle,VisionHotkeys.boostToggle());
        option("Лимит чанков",()->VisionRenderBoost.chunkCap+" чанков",VisionRenderBoost::cycleCap,VisionHotkeys.boostCap());
        option("Целевой FPS",()->Integer.toString(VisionRenderBoost.targetFps),VisionRenderBoost::cycleTargetFps,VisionHotkeys.performanceValue(3));
        String[] values={"Радиус руд","Проверок за тик","Максимум контуров"};
        for(int i=0;i<3;i++){final int id=i;option(values[i],()->Integer.toString(switch(id){case 0->VisionPerformance.oreRadius;case 1->VisionPerformance.checksPerTick;default->VisionPerformance.outlineLimit;}),()->VisionHotkeys.cyclePerformanceValue(id),VisionHotkeys.performanceValue(i));}
        String[] switches={"Адаптивные бюджеты","Лимит HP на спавне","Частота фоновых частиц"};
        for(int i=0;i<3;i++){final int id=i;option(switches[i],()->VisionHotkeys.performanceEnabled(id)?"ВКЛ":"ВЫКЛ",()->VisionHotkeys.togglePerformance(id),VisionHotkeys.performanceToggle(i));}
        option("Детализация эффектов",VisionAppearance::quality,VisionAppearance::cycleDetail);
    }
}
