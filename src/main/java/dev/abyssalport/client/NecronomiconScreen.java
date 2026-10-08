package dev.abyssalport.client;

import dev.abyssalport.item.NecronomiconItem;
import dev.abyssalport.research.EarlyResearch;
import dev.abyssalport.energy.PlaceOfPowerLayout;
import net.minecraft.core.Direction;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** Read-only field guide with server-supplied, player-scoped discovery status. */
@OnlyIn(Dist.CLIENT)
public final class NecronomiconScreen extends Screen {
    private static final int PAGE_COUNT = 30;
    private static final int PANEL = 0xFFDDD0B3;
    private static final int BORDER = 0xFF261E22;
    private static final int INK = 0xFF2A2220;
    private static final int ACCENT = 0xFF673843;
    private final ItemStack book;
    private final int discoveries;
    private int page;
    private int scrollOffset;
    private int maxScroll;
    private int contentBottom;
    private Button previous;
    private Button next;

    public NecronomiconScreen(ItemStack book, int discoveries) {
        super(Component.translatable("screen.abyssalport.necronomicon"));
        this.book = book;
        this.discoveries = discoveries & EarlyResearch.ALL;
    }

    @Override
    protected void init() {
        int left = (width - panelWidth()) / 2;
        int bottom = (height + panelHeight()) / 2;
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> changePage(-1))
                .bounds(left + 18, bottom - 32, 32, 20).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> changePage(1))
                .bounds(left + panelWidth() - 50, bottom - 32, 32, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(left + (panelWidth() - 70) / 2, bottom - 32, 70, 20).build());
        updateButtons();
    }

    private void changePage(int direction) {
        page = Math.clamp(page + direction, 0, PAGE_COUNT - 1);
        scrollOffset = 0;
        maxScroll = 0;
        updateButtons();
    }

    private void updateButtons() {
        previous.active = page > 0;
        next.active = page + 1 < PAGE_COUNT;
    }

    private int panelWidth() {
        return Math.min(354, width - 20);
    }

    private int panelHeight() {
        return Math.min(236, height - 20);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        graphics.fill(left - 2, top - 2, left + panelWidth + 2, top + panelHeight + 2, BORDER);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, PANEL);
        graphics.fill(left + 12, top + 37, left + panelWidth - 12, top + 38, ACCENT);
        graphics.drawCenteredString(font, title, width / 2, top + 11, INK);
        graphics.drawCenteredString(font, Component.translatable("screen.abyssalport.page." + page),
                width / 2, top + 26, ACCENT);
        int textY = top + 50;
        contentBottom = textY;
        graphics.enableScissor(left + 16, top + 44, left + panelWidth - 16, top + panelHeight - 55);
        graphics.pose().pushPose();
        graphics.pose().translate(0, -scrollOffset, 0);
        if (!EarlyResearch.pageAvailable(page, discoveries)) {
            drawParagraph(graphics, "screen.abyssalport.locked." + page,
                    left + 20, textY, panelWidth - 40);
        } else if (page == 0) {
            textY = drawParagraph(graphics, "screen.abyssalport.intro", left + 20, textY, panelWidth - 40);
            textY += 9;
            graphics.drawString(font, Component.translatable("tooltip.abyssalport.potential_energy",
                    (int) NecronomiconItem.getPotentialEnergy(book), NecronomiconItem.capacity(book)),
                    left + 20, textY, ACCENT, false);
            graphics.drawString(font, Component.translatable("screen.abyssalport.discoveries",
                    Integer.bitCount(discoveries), Integer.bitCount(EarlyResearch.ALL)),
                    left + 20, textY + 14, ACCENT, false);
            contentBottom = Math.max(contentBottom, textY + 24);
        } else if (page == 1) {
            textY = drawParagraph(graphics, "screen.abyssalport.energy", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.energy_note", left + 20, textY + 10,
                    panelWidth - 40);
            textY=drawParagraph(graphics, "screen.abyssalport.shoggoth_lairs", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.shoggoth_growth", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 2) {
            drawParagraph(graphics, "screen.abyssalport.ground", left + 20, textY, panelWidth - 40);
            drawGroundDiagram(graphics, width / 2, top + Math.min(124, panelHeight - 80),
                    panelHeight < 210 ? 8 : 11);
        } else if (page == 3) {
            textY = drawParagraph(graphics, "screen.abyssalport.scroll", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.scroll_note", left + 20, textY + 10,
                    panelWidth - 40);
        } else if (page == 4) {
            textY = drawParagraph(graphics, "screen.abyssalport.shadow_materials", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.shadow_materials_note", left + 20, textY + 10,
                    panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.shadow_family.guide", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 5) {
            textY = drawParagraph(graphics, "screen.abyssalport.collector", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.collector_note", left + 20, textY + 10,
                    panelWidth - 40);
        } else if (page == 6) {
            textY = drawParagraph(graphics, "screen.abyssalport.network", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.network_note", left + 20, textY + 10,
                    panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.energy_pedestal", left + 20, textY + 10, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.energy_container_ritual", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.sacrificial_altar", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 7) {
            textY = drawParagraph(graphics, "screen.abyssalport.darklands", left + 20, textY,
                    panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.darklands_note", left + 20, textY + 10,
                    panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.darklands.wood", left + 20, textY + 10, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.darklands.variants", left + 20, textY + 10, panelWidth - 40);
            textY=drawParagraph(graphics, "screen.abyssalport.darklands.ruins", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics,"screen.abyssalport.animal_ecology.guide",left+20,textY+10,panelWidth-40);
        } else if (page == 8) {
            textY = drawParagraph(graphics, "screen.abyssalport.abyssalnite", left + 20, textY,
                    panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.abyssalnite_note", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 9) {
            textY = drawParagraph(graphics, "screen.abyssalport.coralium", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.coralium_note", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 10) {
            textY = drawParagraph(graphics, "screen.abyssalport.infusion", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.infusion_note", left + 20, textY + 10, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.state_transformer.guide", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.energy_depositioner.guide", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 11) {
            textY = drawParagraph(graphics, "screen.abyssalport.sacrifice", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.sacrifice_note", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 12) {
            textY = drawParagraph(graphics, "screen.abyssalport.gateway", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.gateway_note", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 13) {
            textY = drawParagraph(graphics, "screen.abyssalport.wasteland", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.wasteland_note", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.wasteland_creatures", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 14) {
            textY = drawParagraph(graphics, "screen.abyssalport.wasteland_mining", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.refined_coralium_note", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 15) {
            textY = drawParagraph(graphics, "screen.abyssalport.rending", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.rending_note", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 16) {
            textY = drawParagraph(graphics, "screen.abyssalport.wasteland_book", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.wasteland_book_note", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.wasteland_rituals", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 17) {
            textY = drawParagraph(graphics, "screen.abyssalport.asorah", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.asorah_fight", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 18) {
            textY = drawParagraph(graphics, "screen.abyssalport.stronghold", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.stronghold_note", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 19) {
            textY = drawParagraph(graphics, "screen.abyssalport.dreadlands_key", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.dreadlands_key_note", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 20) {
            textY = drawParagraph(graphics, "screen.abyssalport.dreadlands", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.dreadlands_note", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.dreadlands.ecology", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 21) {
            textY = drawParagraph(graphics, "screen.abyssalport.dreadlands_book", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.dreadlands_book_note", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.dreadlands_rituals", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 22) {
            textY = drawParagraph(graphics, "screen.abyssalport.transmutator", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.transmutator_fuel", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.transmutator_automation", left + 20, textY + 10, panelWidth - 40);
        } else if (page == 23) {
            textY = drawParagraph(graphics, "screen.abyssalport.dreadguard", left + 20, textY, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.dreadium", left + 20, textY + 10, panelWidth - 40);
        } else if(page==24) {
            textY = drawParagraph(graphics, "screen.abyssalport.crystallizer", left + 20, textY, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.crystallizer_fractions", left + 20, textY + 10, panelWidth - 40);
            textY = drawParagraph(graphics, "screen.abyssalport.crystallizer_fuel", left + 20, textY + 10, panelWidth - 40);
            drawParagraph(graphics, "screen.abyssalport.crystal_processing", left + 20, textY + 10, panelWidth - 40);
        } else if(page==25) {
            textY=drawParagraph(graphics,"screen.abyssalport.crystal_bags",left+20,textY,panelWidth-40);
            drawParagraph(graphics,"screen.abyssalport.crystal_bags.upgrade",left+20,textY+10,panelWidth-40);
        } else if(page==26) {
            drawParagraph(graphics,"screen.abyssalport.ghouls.guide",left+20,textY,panelWidth-40);
        } else {
            var layout=PlaceOfPowerLayout.values()[page-27];
            textY=drawParagraph(graphics,"screen.abyssalport.power.materials."+layout.id(),left+20,textY,panelWidth-40);
            textY=drawParagraph(graphics,"screen.abyssalport.power.diagram",left+20,textY+10,panelWidth-40);
            textY=drawPowerDiagram(graphics,layout,left+20,textY+10);
            textY=drawParagraph(graphics,"screen.abyssalport.power.guide",left+20,textY+10,panelWidth-40);
            drawParagraph(graphics,"screen.abyssalport.disruptions.guide",left+20,textY+10,panelWidth-40);
        }
        graphics.pose().popPose();
        graphics.disableScissor();
        maxScroll = Math.max(0, contentBottom - (top + panelHeight - 55));
        scrollOffset = Math.min(scrollOffset, maxScroll);
        if (maxScroll > 0) {
            int trackHeight = Math.max(1, panelHeight - 99);
            int thumbHeight = Math.max(12, trackHeight * trackHeight / (trackHeight + maxScroll));
            int thumbTop = top + 44 + (trackHeight - thumbHeight) * scrollOffset / maxScroll;
            graphics.fill(left + panelWidth - 11, top + 44, left + panelWidth - 9, top + panelHeight - 55, BORDER);
            graphics.fill(left + panelWidth - 11, thumbTop, left + panelWidth - 9, thumbTop + thumbHeight, ACCENT);
        }
        graphics.drawCenteredString(font, (page + 1) + " / " + PAGE_COUNT, width / 2,
                top + panelHeight - 48, ACCENT);
        // Screen.render() would apply the background blur again over the finished page.
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private int drawParagraph(GuiGraphics graphics, String key, int x, int y, int maxWidth) {
        List<FormattedCharSequence> lines = font.split(Component.translatable(key), maxWidth);
        for (FormattedCharSequence line : lines) {
            graphics.drawString(font, line, x, y, INK, false);
            y += 11;
        }
        contentBottom = Math.max(contentBottom, y);
        return y;
    }

    private int drawPowerDiagram(GuiGraphics graphics,PlaceOfPowerLayout layout,int x,int y) {
        int minX=layout.pieces().stream().mapToInt(piece->piece.offset().getX()).min().orElse(0);
        int maxX=layout.pieces().stream().mapToInt(piece->piece.offset().getX()).max().orElse(0);
        int minZ=layout.pieces().stream().mapToInt(piece->piece.offset().getZ()).min().orElse(0);
        int maxZ=layout.pieces().stream().mapToInt(piece->piece.offset().getZ()).max().orElse(0);
        for(int layer=layout.bottomLayer();layer<=layout.topLayer();layer++) {
            graphics.drawString(font,Component.translatable("screen.abyssalport.power.layer",layer),x,y,ACCENT,false);
            y+=14;
            for(int row=minZ;row<=maxZ;row++) for(int col=minX;col<=maxX;col++) {
                int px=x+(col-minX)*29,py=y+(row-minZ)*30;
                graphics.fill(px,py,px+27,py+28,0x18342A22);
            }
            for(var piece:layout.pieces()) if(piece.offset().getY()==layer) {
                int px=x+(piece.offset().getX()-minX)*29,py=y+(piece.offset().getZ()-minZ)*30;
                graphics.renderItem(new ItemStack(piece.previewState(Direction.EAST).getBlock()),px+5,py+1);
                String label=switch(piece.material()) {
                    case LOWER_STAIR -> piece.facing()==Direction.EAST?"E B":"W B";
                    case UPPER_STAIR -> piece.facing()==Direction.EAST?"E T":"W T";
                    case LOWER_SLAB -> "B";
                    case UPPER_SLAB -> "T";
                    default -> piece.offset().equals(net.minecraft.core.BlockPos.ZERO)?"*":"";
                };
                graphics.drawCenteredString(font,label,px+13,py+18,INK);
            }
            y+=(maxZ-minZ+1)*30+12;
        }
        contentBottom=Math.max(contentBottom,y); return y;
    }

    private void drawGroundDiagram(GuiGraphics graphics, int centerX, int centerY, int rowSpacing) {
        contentBottom = Math.max(contentBottom, centerY + 3 * rowSpacing + 7);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                boolean altar = x == 0 && z == 0;
                boolean cardinal = (Math.abs(x) == 3 && z == 0) || (Math.abs(z) == 3 && x == 0);
                boolean diagonal = Math.abs(x) == 2 && Math.abs(z) == 2;
                if (altar || cardinal || diagonal) {
                    int px = centerX + x * 16;
                    int py = centerY + z * rowSpacing;
                    graphics.fill(px - 5, py - 5, px + 6, py + 6, altar ? ACCENT : BORDER);
                    graphics.drawCenteredString(font, altar ? "A" : "P", px, py - 4, PANEL);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (maxScroll > 0) {
            scrollOffset = Math.clamp(scrollOffset - (int) Math.round(vertical * 22), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
