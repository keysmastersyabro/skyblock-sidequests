package dev.sidequests.client;

import dev.sidequests.core.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class QuestScreen extends Screen {
    private final SidequestsClient state = SidequestsClient.STATE;
    private int page;
    private int rows, left, boardWidth;
    private List<Quest> visible = List.of();
    private List<Quest> lastQuests;
    private boolean lastBusy;

    public QuestScreen() { super(Component.literal("SkyBlock Sidequests")); }
    @Override protected void init() {
        clearWidgets();
        boardWidth = Math.min(620, width - 24);
        left = (width - boardWidth) / 2;
        rows = Math.max(1, (height - 200) / 57);
        int pages = Math.max(1, (state.board.quests().size() + rows - 1) / rows);
        page = Math.min(page, pages - 1);
        int start = page * rows;
        visible = state.board.quests().subList(start, Math.min(start + rows, state.board.quests().size()));
        lastQuests = state.board.quests();
        lastBusy = state.busy;

        add("Profile: " + state.profileName(), left, 39, boardWidth - 184, () -> { state.nextProfile(); page = 0; init(); });
        add("Refresh", left + boardWidth - 178, 39, 82, state::refresh).active = !state.busy && !state.apiKey.isBlank();
        add("Connect", left + boardWidth - 90, 39, 90, () -> minecraft.gui.setScreen(new ConnectScreen(this)));
        String[] names = {"Quick wins", "Combat", "Money", "Completion"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            int tabWidth = (boardWidth - 12) / 4;
            String label = (state.focus.ordinal() == i ? "> " : "") + names[i];
            add(label, left + i * (tabWidth + 4), 68, tabWidth, () -> {
                state.focus = QuestEngine.Focus.values()[index]; state.regenerate(); page = 0; init();
            });
        }
        for (int i = 0; i < visible.size(); i++) {
            Quest q = visible.get(i);
            add(q.id().equals(state.pinned) ? "Pinned" : "Pin", left + boardWidth - 62,
                    114 + i * 57, 54, () -> { state.pinned = q.id().equals(state.pinned) ? "" : q.id(); init(); });
        }
        int foot = height - 77;
        add("<", left, foot, 30, () -> { page = Math.max(0, page - 1); init(); }).active = page > 0;
        add(">", left + 35, foot, 30, () -> { page++; init(); }).active = page + 1 < pages;
        add("Demo", left + boardWidth - 150, foot, 70, () -> { state.loadDemo(); page = 0; init(); }).active = !state.busy;
        add("Done", left + boardWidth - 74, foot, 74, this::onClose);
    }

    private Button add(String text, int x, int y, int w, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text), b -> action.run()).bounds(x, y, w, 20).build());
    }
    @Override public void tick() {
        if (lastQuests != state.board.quests() || lastBusy != state.busy) init();
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float dt) {
        // The board draws its own backdrop before its content and widgets.
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        g.fill(0, 0, width, height, 0xF0101520);
        g.text(font, "SKYBLOCK / SIDEQUESTS", left, 15, 0xFFFFD787, true);
        String time = state.updated();
        g.text(font, time, left + boardWidth - font.width(time), 17, 0xFFB0B7C8, false);
        if (visible.isEmpty()) {
            String text = state.snapshot == null ? "Your next small adventure starts here." : "No goals available for this focus.";
            g.text(font, text, left + 12, 119, 0xFFFFFFFF, false);
            g.text(font, "Connect your profile, switch focus, or explore the demo.", left + 12, 138, 0xFFADB4C7, false);
        }
        for (int i = 0; i < visible.size(); i++) {
            Quest q = visible.get(i);
            int y = 104 + i * 57;
            g.fill(left, y, left + boardWidth, y + 52, q.id().equals(state.pinned) ? 0xD52B3544 : 0xD519202E);
            int available = boardWidth - 82;
            g.text(font, font.plainSubstrByWidth(q.title(), available), left + 9, y + 6, 0xFFFFD787, false);
            g.text(font, font.plainSubstrByWidth(q.detail(), available), left + 9, y + 18, 0xFFCED4E2, false);
            String progress = q.counter() + "  |  " + q.remaining() + " to go";
            g.text(font, font.plainSubstrByWidth(progress, available), left + 9, y + 30, 0xFF9AA9BD, false);
            g.fill(left + 9, y + 44, left + boardWidth - 9, y + 47, 0xFF343E50);
            g.fill(left + 9, y + 44, left + 9 + (int)((boardWidth - 18) * q.progress()), y + 47, 0xFF67D9B1);
        }
        int foot = height - 77;
        int totalPages = Math.max(1, (state.board.quests().size() + rows - 1) / rows);
        g.text(font, (page + 1) + " / " + totalPages + "  |  " + state.board.quests().size() + " goals",
                left + 74, foot + 6, 0xFFCED4E2, false);
        g.text(font, font.plainSubstrByWidth(state.status, boardWidth), left, height - 50, 0xFFFFD787, false);
        int line = height - 36;
        for (String notice : state.board.notices()) {
            for (var wrapped : font.split(Component.literal(notice), boardWidth)) {
                if (line > height - 22) break;
                g.text(font, wrapped, left, line, 0xFFADB4C7, false);
                line += 10;
            }
            if (line > height - 22) break;
        }
        g.text(font, "Unofficial mod. Not affiliated with Hypixel or Mojang. Manual snapshots only.", left, height - 12, 0xFF808A9B, false);
        super.extractRenderState(g, mx, my, dt);
    }
}
