package com.visionmod;

public final class VisionAppearanceScreen extends VisionOptionsScreen {
    public VisionAppearanceScreen(){super("ВНЕШНИЙ ВИД");}
    @Override protected void options(){
        option("Цветовая тема",()->"ПАЛИТРА "+(VisionMenuTheme.palette()+1),()->VisionMenuTheme.setPalette(VisionMenuTheme.palette()+1));
        option("Детализация эффектов",VisionAppearance::quality,VisionAppearance::cycleDetail);
        option("Непрозрачность HUD",()->Math.round(VisionAppearance.opacity*100f/255)+"%",VisionAppearance::cycleOpacity);
        option("Плавные анимации",()->VisionAppearance.animations?"ВКЛ":"ВЫКЛ",VisionAppearance::toggleAnimations);
        option("Яркость FullBright",()->Integer.toString(VisionAppearance.brightness),VisionAppearance::cycleBrightness);
        option("Масштаб предметов в руке",()->Math.round(VisionAppearance.handScale*100)+"%",VisionAppearance::cycleHandScale);
        option("Позиции HUD",()->"Сбросить · двигать можно через чат",VisionHudLayout::reset);
    }
}
