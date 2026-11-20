package dev.sidequests.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ConnectScreen extends Screen {
    private final Screen parent;
    private EditBox key;
    private int left, span;
    private int fieldY;
    private static final String[] HELP = {"Create an application at developer.hypixel.net.",
            "Paste its API key below. Your signed-in Minecraft account is used.",
            "The key stays in memory and is forgotten when Minecraft closes."};
    public ConnectScreen(Screen parent) { super(Component.literal("Connect your profile")); this.parent = parent; }
    @Override protected void init() {
        span = Math.min(420, width - 36);
        left = (width - span) / 2;
        int helpLines = 0;
        for (String line : HELP) helpLines += font.split(Component.literal(line), span).size();
        fieldY = Math.max(92, 47 + helpLines * 10 + 18);
        key = new EditBox(font, left, fieldY, span, 22, Component.literal("Hypixel application API key")) {
            @Override protected MutableComponent createNarrationMessage() {
                return Component.literal("Hypixel application API key. Value hidden.");
            }
        };
        key.setMaxLength(64);
        key.setValue(SidequestsClient.STATE.apiKey);
        key.addFormatter((value, start) -> Component.literal("*".repeat(value.length())).getVisualOrderText());
        key.setResponder(value -> SidequestsClient.STATE.apiKey = value);
        addRenderableWidget(key);
        setInitialFocus(key);
        addRenderableWidget(Button.builder(Component.literal("Connect"), b -> {
            SidequestsClient.STATE.apiKey = key.getValue().trim();
            SidequestsClient.STATE.refresh();
            onClose();
        }).bounds(left, fieldY + 37, (span - 8) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Back"), b -> onClose())
                .bounds(left + (span + 8) / 2, fieldY + 37, (span - 8) / 2, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Forget key"), b -> {
            key.setValue(""); SidequestsClient.STATE.apiKey = "";
        }).bounds(left, fieldY + 66, span, 20).build());
    }
    @Override public void onClose() { minecraft.gui.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        super.extractRenderState(g, mx, my, dt);
        g.text(font, "CONNECT YOUR PROFILE", left, 26, 0xFFFFD787, true);
        int y = 47;
        for (String line : HELP) {
            for (var wrapped : font.split(Component.literal(line), span)) {
                g.text(font, wrapped, left, y, 0xFFCED4E2, false); y += 10;
            }
        }
        g.text(font, "API key (hidden; Ctrl+V to paste)", left, fieldY - 11, 0xFFADB4C7, false);
    }
}
