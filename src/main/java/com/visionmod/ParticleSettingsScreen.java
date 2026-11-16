package com.visionmod;

public final class ParticleSettingsScreen extends VisionOptionsScreen {
    public ParticleSettingsScreen(){super("ЧАСТИЦЫ");}
    @Override protected void options(){
        String[] names={"Разрушение блоков","Бег","Портал / эндер-сундук","Все ванильные частицы"};
        for(int i=0;i<4;i++){final int id=i;option(names[i],()->(VisionHotkeys.particleEnabled(id)?"СКРЫТЫ":"ВИДИМЫ")+" · "+VisionHotkeys.label(VisionHotkeys.particle(id)),()->VisionHotkeys.toggleParticle(id),VisionHotkeys.particle(i));}
        option("Плотность",VisionExtras::ambientDensityLabel,VisionExtras::cycleAmbientDensity);
        option("Размещение",VisionExtras::ambientStyleLabel,VisionExtras::cycleAmbientStyle);
    }
}
