package com.visionmod;

import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Paste words or time a local LRC from inside the game. */
final class VisionManualLyricsScreen extends VisionPanelScreen {
    private final VisionLyricsScreen parent;
    private EditBox position;
    private String message = "";

    VisionManualLyricsScreen(VisionLyricsScreen parent) {
        super("ТЕКСТ И СИНХРОНИЗАЦИЯ");
        this.parent = parent;
    }

    @Override protected void build() {
        String previous = position == null ? formatTime(VisionLyricsSource.playbackPosition()) : position.getValue();
        position = new EditBox(font, x+16, layout.bodyTop()+62, 70, 20, Component.literal("Позиция песни в минутах и секундах"));
        position.setMaxLength(10);
        position.setValue(previous);
        addRenderableWidget(position);
    }

    @Override protected void draw(GuiGraphics g, int mx, int my, float delta) {
        int top = layout.bodyTop();
        int half = (w-42)/2;
        boolean imported = VisionLyricsSource.hasImport();
        line(g, "Скопируй слова песни или LRC с таймкодами.", top, VisionMenuTheme.MUTED);
        button(g, x+16, top+16, half, 22, "Вставить текст", imported, mx, my);
        button(g, x+26+half, top+16, half, 22, "Убрать вставленный текст", false, mx, my);
        line(g, VisionLyricsSource.manualClock()
            ? "Ручная позиция  " + formatTime(VisionLyricsSource.playbackPosition())
            : "Позиция песни  ·  минуты:секунды", top+47, VisionMenuTheme.WHITE);

        int controls = (w-122)/2;
        button(g, x+96, top+61, controls, 22, VisionLyricsSource.manualPlaying() ? "Пауза" : "Пуск с позиции", VisionLyricsSource.manualPlaying(), mx, my);
        button(g, x+106+controls, top+61, controls, 22, "Авто", !VisionLyricsSource.manualClock(), mx, my);
        line(g, message.isEmpty() ? "Ручной отсчёт нужен, когда плеер не передаёт позицию." : message, top+91, VisionMenuTheme.CYAN);
        if (imported) line(g, "Вставленный текст действует до смены песни.", top+108, VisionMenuTheme.MUTED);
    }

    private void line(GuiGraphics g, String text, int yy, int color) {
        VisionFonts.fit(g, font, text, x+16, yy, w-32, color);
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        int top = layout.bodyTop(), half = (w-42)/2, controls = (w-122)/2;
        double mx = event.x(), my = event.y();
        if (event.button() == 0) {
            if (hit(mx, my, x+16, top+16, half, 22)) {
                message = VisionLyricsSource.importText(minecraft.keyboardHandler.getClipboard())
                    ? VisionLyricsSource.hasTimedLyrics() ? "LRC загружен." : "Слова доступны на экране World Lyrics."
                    : "Скопируй непустой текст или LRC до 200 000 символов.";
                position.setValue(formatTime(VisionLyricsSource.playbackPosition()));
                return true;
            }
            if (hit(mx, my, x+26+half, top+16, half, 22)) {
                VisionLyricsSource.clearImport();
                position.setValue(formatTime(VisionLyricsSource.playbackPosition()));
                message = "Вставленный текст убран.";
                return true;
            }
            if (hit(mx, my, x+96, top+61, controls, 22)) {
                changeClock();
                return true;
            }
            if (hit(mx, my, x+106+controls, top+61, controls, 22)) {
                VisionLyricsSource.useAutomaticClock();
                position.setValue(formatTime(VisionLyricsSource.playbackPosition()));
                message = "Выбрана автоматическая позиция.";
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    private void changeClock() {
        boolean playing = VisionLyricsSource.manualPlaying();
        long time = playing ? VisionLyricsSource.playbackPosition() : parseTime(position.getValue());
        if (time < 0) {
            message = "Укажи позицию в формате минуты:секунды, например 01:25.";
        } else if (!VisionLyricsSource.setManualClock(time, !playing)) {
            message = "Для ручного отсчёта нужен текст с таймкодами LRC.";
        } else {
            position.setValue(formatTime(time));
            message = playing ? "Отсчёт на паузе." : "Отсчёт запущен.";
        }
    }

    private static long parseTime(String text) {
        if (!text.matches("[0-9]{1,3}:[0-5][0-9](?:[.,][0-9]{1,3})?")) return -1;
        int separator = text.indexOf(':');
        return Integer.parseInt(text.substring(0, separator))*60_000L
            + Math.round(Double.parseDouble(text.substring(separator+1).replace(',', '.'))*1_000);
    }

    private static String formatTime(long time) {
        time = Math.max(0, time);
        return String.format(Locale.ROOT, "%02d:%02d.%03d", time/60_000, time/1_000%60, time%1_000);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
}
