package com.mateuslopees.cobblecoins.client.screen;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mateuslopees.cobblecoins.CobbleCoins;
import com.mateuslopees.cobblecoins.client.ClientBankData;
import com.mateuslopees.cobblecoins.network.NetworkHandler;
import com.mateuslopees.cobblecoins.network.packet.ShopPurchasePacket;
import com.mateuslopees.cobblecoins.network.packet.ShopSellPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.*;

public class ShopScreen extends Screen {
    private static final Gson GSON = new Gson();
    
    // Layout constants
    private static final int GUI_WIDTH = 340;
    private static final int GUI_HEIGHT = 220;
    private static final int CATEGORY_WIDTH = 90;
    private static final int ITEM_SIZE = 24;
    private static final int ITEMS_PER_ROW = 6;
    private static final int ITEMS_PER_PAGE = 18; // 6x3 grid
    
    // Colors
    private static final int COLOR_BG_DARK = 0xF0141414;
    private static final int COLOR_BG_PANEL = 0xF01E1E1E;
    private static final int COLOR_BG_HOVER = 0xFF2A2A2A;
    private static final int COLOR_BORDER = 0xFF3D3D3D;
    private static final int COLOR_BORDER_LIGHT = 0xFF4A4A4A;
    private static final int COLOR_ACCENT = 0xFFFFD700;
    private static final int COLOR_ACCENT_BUY = 0xFF4CAF50;
    private static final int COLOR_ACCENT_SELL = 0xFFFF9800;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_TEXT_DIM = 0xFFAAAAAA;
    private static final int COLOR_TEXT_ERROR = 0xFFFF5555;
    private static final int COLOR_SELECTED = 0xFF3D5AFE;
    
    private final List<ShopEntry> buyEntries = new ArrayList<>();
    private final List<ShopEntry> sellEntries = new ArrayList<>();
    private final Map<String, List<ShopEntry>> buyCategories = new LinkedHashMap<>();
    private final Map<String, List<ShopEntry>> sellCategories = new LinkedHashMap<>();
    
    private int guiLeft;
    private int guiTop;
    private int currentPage = 0;
    private boolean showingBuyShop = true;
    private String selectedCategory = null;
    private ShopEntry selectedEntry = null;
    private int quantity = 1;
    private int hoveredItemIndex = -1;
    private int hoveredCategoryIndex = -1;
    
    // Scroll position for categories
    private int categoryScrollOffset = 0;
    private int maxVisibleCategories = 8;

    public ShopScreen(String shopData) {
        super(Component.translatable("screen.cobblecoins.shop"));
        parseShopData(shopData);
    }

    private void parseShopData(String shopData) {
        try {
            JsonObject json = GSON.fromJson(shopData, JsonObject.class);
            
            if (json.has("buy")) {
                JsonArray buyArray = json.getAsJsonArray("buy");
                for (int i = 0; i < buyArray.size(); i++) {
                    JsonObject entry = buyArray.get(i).getAsJsonObject();
                    ShopEntry shopEntry = ShopEntry.fromJson(entry);
                    buyEntries.add(shopEntry);
                    buyCategories.computeIfAbsent(shopEntry.category, k -> new ArrayList<>()).add(shopEntry);
                }
            }
            
            if (json.has("sell")) {
                JsonArray sellArray = json.getAsJsonArray("sell");
                for (int i = 0; i < sellArray.size(); i++) {
                    JsonObject entry = sellArray.get(i).getAsJsonObject();
                    ShopEntry shopEntry = ShopEntry.fromJson(entry);
                    sellEntries.add(shopEntry);
                    sellCategories.computeIfAbsent(shopEntry.category, k -> new ArrayList<>()).add(shopEntry);
                }
            }
            
            // Select first category by default
            if (!buyCategories.isEmpty()) {
                selectedCategory = buyCategories.keySet().iterator().next();
            }
        } catch (Exception e) {
            CobbleCoins.LOGGER.error("Failed to parse shop data", e);
        }
    }

    @Override
    protected void init() {
        super.init();
        guiLeft = (width - GUI_WIDTH) / 2;
        guiTop = (height - GUI_HEIGHT) / 2;
    }

    private List<ShopEntry> getCurrentEntries() {
        Map<String, List<ShopEntry>> categories = showingBuyShop ? buyCategories : sellCategories;
        if (selectedCategory != null && categories.containsKey(selectedCategory)) {
            return categories.get(selectedCategory);
        }
        return showingBuyShop ? buyEntries : sellEntries;
    }

    private Map<String, List<ShopEntry>> getCurrentCategories() {
        return showingBuyShop ? buyCategories : sellCategories;
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
        // Don't render blur
    }

    @Override
    protected void renderMenuBackground(GuiGraphics graphics) {
        // Don't render default menu background
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Simple dark overlay without blur
        graphics.fill(0, 0, width, height, 0x88000000);
        
        // Main background
        drawPanel(graphics, guiLeft, guiTop, GUI_WIDTH, GUI_HEIGHT, COLOR_BG_DARK, COLOR_BORDER);
        
        // Draw header
        renderHeader(graphics, mouseX, mouseY);
        
        // Draw category sidebar
        renderCategorySidebar(graphics, mouseX, mouseY);
        
        // Draw items grid
        renderItemsGrid(graphics, mouseX, mouseY);
        
        // Draw detail panel (bottom)
        renderDetailPanel(graphics, mouseX, mouseY);
        
        // Draw balance bar
        renderBalanceBar(graphics);
        
        super.render(graphics, mouseX, mouseY, partialTick);
        
        // Render item tooltip
        renderItemTooltip(graphics, mouseX, mouseY);
    }

    private void renderHeader(GuiGraphics graphics, int mouseX, int mouseY) {
        int headerY = guiTop + 5;
        int tabWidth = 60;
        int tabHeight = 20;
        
        // Shop title
        graphics.drawString(font, "§l⬢ COBBLESHOP", guiLeft + 10, headerY + 5, COLOR_ACCENT);
        
        // Tab buttons
        int tabX = guiLeft + GUI_WIDTH - tabWidth * 2 - 15;
        
        // Buy tab
        boolean buyHovered = isMouseOver(mouseX, mouseY, tabX, headerY, tabWidth, tabHeight);
        int buyColor = showingBuyShop ? COLOR_ACCENT_BUY : (buyHovered ? COLOR_BG_HOVER : COLOR_BG_PANEL);
        drawPanel(graphics, tabX, headerY, tabWidth, tabHeight, buyColor, showingBuyShop ? COLOR_ACCENT_BUY : COLOR_BORDER);
        graphics.drawCenteredString(font, "BUY", tabX + tabWidth / 2, headerY + 6, showingBuyShop ? 0xFF000000 : COLOR_TEXT);
        
        // Sell tab
        int sellTabX = tabX + tabWidth + 5;
        boolean sellHovered = isMouseOver(mouseX, mouseY, sellTabX, headerY, tabWidth, tabHeight);
        int sellColor = !showingBuyShop ? COLOR_ACCENT_SELL : (sellHovered ? COLOR_BG_HOVER : COLOR_BG_PANEL);
        drawPanel(graphics, sellTabX, headerY, tabWidth, tabHeight, sellColor, !showingBuyShop ? COLOR_ACCENT_SELL : COLOR_BORDER);
        graphics.drawCenteredString(font, "SELL", sellTabX + tabWidth / 2, headerY + 6, !showingBuyShop ? 0xFF000000 : COLOR_TEXT);
    }

    private void renderCategorySidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int sidebarX = guiLeft + 5;
        int sidebarY = guiTop + 30;
        int sidebarHeight = 130;
        
        // Sidebar background
        drawPanel(graphics, sidebarX, sidebarY, CATEGORY_WIDTH, sidebarHeight, COLOR_BG_PANEL, COLOR_BORDER);
        
        // Category header
        graphics.drawString(font, "§nCategories", sidebarX + 5, sidebarY + 4, COLOR_TEXT_DIM);
        
        // Categories list
        Map<String, List<ShopEntry>> categories = getCurrentCategories();
        List<String> categoryNames = new ArrayList<>(categories.keySet());
        
        int catY = sidebarY + 18;
        int catHeight = 14;
        hoveredCategoryIndex = -1;
        
        int visibleStart = categoryScrollOffset;
        int visibleEnd = Math.min(categoryScrollOffset + maxVisibleCategories, categoryNames.size());
        
        for (int i = visibleStart; i < visibleEnd; i++) {
            String category = categoryNames.get(i);
            List<ShopEntry> items = categories.get(category);
            int itemCount = items.size();
            
            int currentCatY = catY + (i - visibleStart) * catHeight;
            boolean hovered = isMouseOver(mouseX, mouseY, sidebarX + 2, currentCatY, CATEGORY_WIDTH - 4, catHeight);
            boolean selected = category.equals(selectedCategory);
            
            if (hovered) hoveredCategoryIndex = i;
            
            // Category background
            if (selected) {
                graphics.fill(sidebarX + 2, currentCatY, sidebarX + CATEGORY_WIDTH - 2, currentCatY + catHeight, COLOR_SELECTED);
            } else if (hovered) {
                graphics.fill(sidebarX + 2, currentCatY, sidebarX + CATEGORY_WIDTH - 2, currentCatY + catHeight, COLOR_BG_HOVER);
            }
            
            // Category name (truncate if needed)
            String displayName = formatCategoryName(category);
            if (font.width(displayName) > CATEGORY_WIDTH - 30) {
                displayName = displayName.substring(0, Math.min(displayName.length(), 8)) + "..";
            }
            graphics.drawString(font, displayName, sidebarX + 5, currentCatY + 3, selected ? COLOR_TEXT : COLOR_TEXT_DIM);
            
            // Item count badge
            String countStr = "§7(" + itemCount + ")";
            graphics.drawString(font, countStr, sidebarX + CATEGORY_WIDTH - font.width(countStr) - 5, currentCatY + 3, COLOR_TEXT_DIM);
        }
        
        // Scroll indicators
        if (categoryScrollOffset > 0) {
            graphics.drawCenteredString(font, "▲", sidebarX + CATEGORY_WIDTH / 2, sidebarY + 14, COLOR_TEXT_DIM);
        }
        if (visibleEnd < categoryNames.size()) {
            graphics.drawCenteredString(font, "▼", sidebarX + CATEGORY_WIDTH / 2, sidebarY + sidebarHeight - 10, COLOR_TEXT_DIM);
        }
    }

    private void renderItemsGrid(GuiGraphics graphics, int mouseX, int mouseY) {
        int gridX = guiLeft + CATEGORY_WIDTH + 10;
        int gridY = guiTop + 30;
        int gridWidth = GUI_WIDTH - CATEGORY_WIDTH - 15;
        int gridHeight = 85;
        
        // Grid background
        drawPanel(graphics, gridX, gridY, gridWidth, gridHeight, COLOR_BG_PANEL, COLOR_BORDER);
        
        List<ShopEntry> entries = getCurrentEntries();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, entries.size());
        
        hoveredItemIndex = -1;
        
        for (int i = startIndex; i < endIndex; i++) {
            ShopEntry entry = entries.get(i);
            int localIndex = i - startIndex;
            int col = localIndex % ITEMS_PER_ROW;
            int row = localIndex / ITEMS_PER_ROW;
            
            int itemX = gridX + 5 + col * (ITEM_SIZE + 4);
            int itemY = gridY + 5 + row * (ITEM_SIZE + 4);
            
            boolean hovered = isMouseOver(mouseX, mouseY, itemX, itemY, ITEM_SIZE, ITEM_SIZE);
            boolean selected = entry == selectedEntry;
            
            if (hovered) hoveredItemIndex = i;
            
            // Item slot background
            int slotColor = selected ? COLOR_SELECTED : (hovered ? COLOR_BG_HOVER : 0xFF2D2D2D);
            int borderColor = selected ? COLOR_ACCENT : (hovered ? COLOR_BORDER_LIGHT : COLOR_BORDER);
            drawPanel(graphics, itemX, itemY, ITEM_SIZE, ITEM_SIZE, slotColor, borderColor);
            
            // Render item
            ItemStack stack = entry.getItemStack();
            graphics.renderItem(stack, itemX + 4, itemY + 4);
            
            // Price indicator (small)
            String priceStr = formatCompactPrice(entry.price);
            int priceWidth = font.width(priceStr);
            graphics.fill(itemX + ITEM_SIZE - priceWidth - 2, itemY + ITEM_SIZE - 9, itemX + ITEM_SIZE, itemY + ITEM_SIZE, 0xCC000000);
            graphics.drawString(font, priceStr, itemX + ITEM_SIZE - priceWidth - 1, itemY + ITEM_SIZE - 8, COLOR_ACCENT, false);
        }
        
        // Page navigation
        int totalPages = (int) Math.ceil(entries.size() / (double) ITEMS_PER_PAGE);
        if (totalPages > 1) {
            String pageStr = (currentPage + 1) + "/" + totalPages;
            int navY = gridY + gridHeight - 12;
            
            // Previous button
            boolean canPrev = currentPage > 0;
            int prevX = gridX + 5;
            graphics.drawString(font, canPrev ? "§e◄" : "§8◄", prevX, navY, COLOR_TEXT);
            
            // Page number
            graphics.drawCenteredString(font, pageStr, gridX + gridWidth / 2, navY, COLOR_TEXT_DIM);
            
            // Next button
            boolean canNext = currentPage < totalPages - 1;
            int nextX = gridX + gridWidth - 15;
            graphics.drawString(font, canNext ? "§e►" : "§8►", nextX, navY, COLOR_TEXT);
        }
    }

    private void renderDetailPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelX = guiLeft + 5;
        int panelY = guiTop + 165;
        int panelWidth = GUI_WIDTH - 10;
        int panelHeight = 50;
        
        // Panel background
        drawPanel(graphics, panelX, panelY, panelWidth, panelHeight, COLOR_BG_PANEL, COLOR_BORDER);
        
        if (selectedEntry == null) {
            graphics.drawCenteredString(font, "§7Select an item to " + (showingBuyShop ? "buy" : "sell"), 
                    panelX + panelWidth / 2, panelY + 20, COLOR_TEXT_DIM);
            return;
        }
        
        // Item icon and name
        ItemStack stack = selectedEntry.getItemStack();
        graphics.renderItem(stack, panelX + 8, panelY + 8);
        graphics.drawString(font, "§f" + selectedEntry.getDisplayName(), panelX + 30, panelY + 8, COLOR_TEXT);
        graphics.drawString(font, "§7Unit price: §e" + formatPrice(selectedEntry.price) + " " + selectedEntry.getCurrencySymbol(), panelX + 30, panelY + 20, COLOR_TEXT_DIM);
        
        // Quantity controls - moved left and reorganized
        int qtyX = panelX + 150;
        int qtyY = panelY + 6;
        
        graphics.drawString(font, "Qty:", qtyX, qtyY + 4, COLOR_TEXT_DIM);
        
        // Minus button
        int minusBtnX = qtyX + 25;
        boolean minusHovered = isMouseOver(mouseX, mouseY, minusBtnX, qtyY, 18, 18);
        drawPanel(graphics, minusBtnX, qtyY, 18, 18, minusHovered ? COLOR_BG_HOVER : COLOR_BG_DARK, COLOR_BORDER);
        graphics.drawCenteredString(font, "-", minusBtnX + 9, qtyY + 5, quantity > 1 ? COLOR_TEXT : COLOR_TEXT_DIM);
        
        // Quantity display
        int qtyDisplayX = minusBtnX + 20;
        drawPanel(graphics, qtyDisplayX, qtyY, 28, 18, COLOR_BG_DARK, COLOR_BORDER);
        graphics.drawCenteredString(font, String.valueOf(quantity), qtyDisplayX + 14, qtyY + 5, COLOR_TEXT);
        
        // Plus button
        int plusBtnX = qtyDisplayX + 30;
        boolean plusHovered = isMouseOver(mouseX, mouseY, plusBtnX, qtyY, 18, 18);
        drawPanel(graphics, plusBtnX, qtyY, 18, 18, plusHovered ? COLOR_BG_HOVER : COLOR_BG_DARK, COLOR_BORDER);
        graphics.drawCenteredString(font, "+", plusBtnX + 9, qtyY + 5, COLOR_TEXT);
        
        // Quick quantity buttons - in a row below
        int quickY = qtyY + 22;
        int[] quickAmounts = {1, 10, 64};
        int quickBtnWidth = 28;
        int quickBtnSpacing = 2;
        int totalQuickWidth = quickAmounts.length * quickBtnWidth + (quickAmounts.length - 1) * quickBtnSpacing;
        int quickStartX = qtyX + 25;
        
        for (int i = 0; i < quickAmounts.length; i++) {
            int btnX = quickStartX + i * (quickBtnWidth + quickBtnSpacing);
            boolean btnHovered = isMouseOver(mouseX, mouseY, btnX, quickY, quickBtnWidth, 14);
            drawPanel(graphics, btnX, quickY, quickBtnWidth, 14, btnHovered ? COLOR_BG_HOVER : COLOR_BG_DARK, COLOR_BORDER);
            graphics.drawCenteredString(font, "x" + quickAmounts[i], btnX + quickBtnWidth / 2, quickY + 3, COLOR_TEXT_DIM);
        }
        
        // Total and confirm button - positioned on the right
        long total = selectedEntry.price * quantity;
        // For bank balance, we can check client-side. For item currency, always allow (server validates)
        boolean canAfford = !showingBuyShop || !selectedEntry.usesBankBalance() || ClientBankData.getBalance() >= total;
        
        int confirmX = panelX + panelWidth - 60;
        int confirmY = panelY + 8;
        int confirmWidth = 52;
        int confirmHeight = 34;
        
        boolean confirmHovered = isMouseOver(mouseX, mouseY, confirmX, confirmY, confirmWidth, confirmHeight);
        int confirmColor = showingBuyShop ? COLOR_ACCENT_BUY : COLOR_ACCENT_SELL;
        if (!canAfford) confirmColor = 0xFF555555;
        else if (confirmHovered) confirmColor = showingBuyShop ? 0xFF66BB6A : 0xFFFFAB40;
        else if (confirmHovered) confirmColor = showingBuyShop ? 0xFF66BB6A : 0xFFFFAB40;
        
        drawPanel(graphics, confirmX, confirmY, confirmWidth, confirmHeight, confirmColor, confirmColor);
        String btnText = showingBuyShop ? "BUY" : "SELL";
        graphics.drawCenteredString(font, "§l" + btnText, confirmX + confirmWidth / 2, confirmY + 5, canAfford ? 0xFF000000 : COLOR_TEXT_DIM);
        
        String totalStr = formatPrice(total) + " " + selectedEntry.getCurrencySymbol();
        graphics.drawCenteredString(font, totalStr, confirmX + confirmWidth / 2, confirmY + 18, canAfford ? 0xFF000000 : COLOR_TEXT_DIM);
        
        if (!canAfford && showingBuyShop) {
            graphics.drawString(font, "§cInsufficient funds!", panelX + 30, panelY + 35, COLOR_TEXT_ERROR);
        }
    }

    private void renderBalanceBar(GuiGraphics graphics) {
        int barX = guiLeft;
        int barY = guiTop + GUI_HEIGHT + 2;
        int barWidth = GUI_WIDTH;
        int barHeight = 18;
        
        // Balance bar background
        drawPanel(graphics, barX, barY, barWidth, barHeight, COLOR_BG_DARK, COLOR_BORDER);
        
        // Coin icon placeholder
        graphics.drawString(font, "§e⬢", barX + 8, barY + 5, COLOR_ACCENT);
        
        // Balance text
        String balanceText = "Balance: §e" + ClientBankData.getFormattedBalance() + " ¢";
        graphics.drawString(font, balanceText, barX + 20, barY + 5, COLOR_TEXT);
        
        // Cart summary (if item selected)
        if (selectedEntry != null) {
            long total = selectedEntry.price * quantity;
            String action = showingBuyShop ? "Cost" : "Earn";
            String currencySymbol = selectedEntry.getCurrencySymbol();
            String cartText = action + ": §e" + formatPrice(total) + " " + currencySymbol + " §7(" + quantity + "x " + selectedEntry.getDisplayName() + ")";
            int cartTextWidth = font.width(cartText.replaceAll("§.", ""));
            graphics.drawString(font, cartText, barX + barWidth - cartTextWidth - 10, barY + 5, COLOR_TEXT_DIM);
        }
    }

    private void renderItemTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (hoveredItemIndex >= 0) {
            List<ShopEntry> entries = getCurrentEntries();
            if (hoveredItemIndex < entries.size()) {
                ShopEntry entry = entries.get(hoveredItemIndex);
                ItemStack stack = entry.getItemStack();
                
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.literal("§f§l" + entry.getDisplayName()));
                tooltip.add(Component.literal("§7Category: §e" + formatCategoryName(entry.category)));
                tooltip.add(Component.literal(""));
                String currencyText = entry.getCurrencySymbol();
                tooltip.add(Component.literal((showingBuyShop ? "§aBuy" : "§6Sell") + " Price: §e" + formatPrice(entry.price) + " " + currencyText));
                tooltip.add(Component.literal("§8Click to select"));
                
                graphics.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int mx = (int) mouseX;
        int my = (int) mouseY;
        
        // Tab clicks
        int headerY = guiTop + 5;
        int tabWidth = 60;
        int tabHeight = 20;
        int tabX = guiLeft + GUI_WIDTH - tabWidth * 2 - 15;
        
        if (isMouseOver(mx, my, tabX, headerY, tabWidth, tabHeight)) {
            if (!showingBuyShop) {
                showingBuyShop = true;
                currentPage = 0;
                selectedEntry = null;
                quantity = 1;
                Map<String, List<ShopEntry>> cats = getCurrentCategories();
                selectedCategory = cats.isEmpty() ? null : cats.keySet().iterator().next();
            }
            return true;
        }
        
        int sellTabX = tabX + tabWidth + 5;
        if (isMouseOver(mx, my, sellTabX, headerY, tabWidth, tabHeight)) {
            if (showingBuyShop) {
                showingBuyShop = false;
                currentPage = 0;
                selectedEntry = null;
                quantity = 1;
                Map<String, List<ShopEntry>> cats = getCurrentCategories();
                selectedCategory = cats.isEmpty() ? null : cats.keySet().iterator().next();
            }
            return true;
        }
        
        // Category clicks
        if (hoveredCategoryIndex >= 0) {
            List<String> categoryNames = new ArrayList<>(getCurrentCategories().keySet());
            if (hoveredCategoryIndex < categoryNames.size()) {
                selectedCategory = categoryNames.get(hoveredCategoryIndex);
                currentPage = 0;
                selectedEntry = null;
            }
            return true;
        }
        
        // Item clicks
        if (hoveredItemIndex >= 0) {
            List<ShopEntry> entries = getCurrentEntries();
            if (hoveredItemIndex < entries.size()) {
                selectedEntry = entries.get(hoveredItemIndex);
                quantity = 1;
            }
            return true;
        }
        
        // Quantity controls
        if (selectedEntry != null) {
            int panelX = guiLeft + 5;
            int panelY = guiTop + 165;
            int qtyX = panelX + 150;
            int qtyY = panelY + 6;
            
            // Minus button
            int minusBtnX = qtyX + 25;
            if (isMouseOver(mx, my, minusBtnX, qtyY, 18, 18)) {
                if (quantity > 1) quantity--;
                return true;
            }
            
            // Plus button
            int plusBtnX = minusBtnX + 48;
            if (isMouseOver(mx, my, plusBtnX, qtyY, 18, 18)) {
                quantity = Math.min(quantity + 1, 64);
                return true;
            }
            
            // Quick quantity buttons
            int quickY = qtyY + 22;
            int[] quickAmounts = {1, 10, 64};
            int quickBtnWidth = 28;
            int quickBtnSpacing = 2;
            for (int i = 0; i < quickAmounts.length; i++) {
                int btnX = qtyX + 25 + i * (quickBtnWidth + quickBtnSpacing);
                if (isMouseOver(mx, my, btnX, quickY, quickBtnWidth, 14)) {
                    quantity = quickAmounts[i];
                    return true;
                }
            }
            
            // Confirm button
            int confirmX = panelX + (GUI_WIDTH - 10) - 60;
            int confirmY = panelY + 8;
            if (isMouseOver(mx, my, confirmX, confirmY, 52, 34)) {
                executePurchase();
                return true;
            }
        }
        
        // Page navigation
        int gridX = guiLeft + CATEGORY_WIDTH + 10;
        int gridY = guiTop + 30;
        int gridWidth = GUI_WIDTH - CATEGORY_WIDTH - 15;
        int gridHeight = 85;
        
        List<ShopEntry> entries = getCurrentEntries();
        int totalPages = (int) Math.ceil(entries.size() / (double) ITEMS_PER_PAGE);
        
        if (totalPages > 1) {
            int navY = gridY + gridHeight - 12;
            
            // Previous
            if (isMouseOver(mx, my, gridX + 5, navY - 2, 15, 12) && currentPage > 0) {
                currentPage--;
                return true;
            }
            
            // Next
            if (isMouseOver(mx, my, gridX + gridWidth - 15, navY - 2, 15, 12) && currentPage < totalPages - 1) {
                currentPage++;
                return true;
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Category scrolling
        int sidebarX = guiLeft + 5;
        int sidebarY = guiTop + 30;
        int sidebarHeight = 130;
        
        if (isMouseOver((int) mouseX, (int) mouseY, sidebarX, sidebarY, CATEGORY_WIDTH, sidebarHeight)) {
            int maxScroll = Math.max(0, getCurrentCategories().size() - maxVisibleCategories);
            if (scrollY > 0 && categoryScrollOffset > 0) {
                categoryScrollOffset--;
            } else if (scrollY < 0 && categoryScrollOffset < maxScroll) {
                categoryScrollOffset++;
            }
            return true;
        }
        
        // Items grid scrolling
        int gridX = guiLeft + CATEGORY_WIDTH + 10;
        int gridY = guiTop + 30;
        int gridWidth = GUI_WIDTH - CATEGORY_WIDTH - 15;
        int gridHeight = 85;
        
        if (isMouseOver((int) mouseX, (int) mouseY, gridX, gridY, gridWidth, gridHeight)) {
            int totalPages = (int) Math.ceil(getCurrentEntries().size() / (double) ITEMS_PER_PAGE);
            if (scrollY > 0 && currentPage > 0) {
                currentPage--;
            } else if (scrollY < 0 && currentPage < totalPages - 1) {
                currentPage++;
            }
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void executePurchase() {
        if (selectedEntry == null) return;
        
        long total = selectedEntry.price * quantity;
        
        if (showingBuyShop) {
            // For item-based currency, we can't validate client-side (inventory is server-side)
            // Just send the packet and let server validate
            if (selectedEntry.usesBankBalance()) {
                // Only do client-side validation for bank balance
                if (ClientBankData.getBalance() >= total) {
                    NetworkHandler.sendToServer(new ShopPurchasePacket(selectedEntry.itemId, quantity));
                    // Optimistic update for bank balance only
                    ClientBankData.setBalance(ClientBankData.getBalance() - total);
                }
            } else {
                // For item currencies, just send the request - server will validate
                NetworkHandler.sendToServer(new ShopPurchasePacket(selectedEntry.itemId, quantity));
            }
        } else {
            NetworkHandler.sendToServer(new ShopSellPacket(-1, quantity));
        }
    }

    private void drawPanel(GuiGraphics graphics, int x, int y, int width, int height, int bgColor, int borderColor) {
        // Background
        graphics.fill(x, y, x + width, y + height, bgColor);
        // Border
        graphics.fill(x, y, x + width, y + 1, borderColor); // Top
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor); // Bottom
        graphics.fill(x, y, x + 1, y + height, borderColor); // Left
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor); // Right
    }

    private boolean isMouseOver(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private String formatCategoryName(String category) {
        if (category == null || category.isEmpty()) return "Misc";
        return category.substring(0, 1).toUpperCase() + category.substring(1).toLowerCase().replace("_", " ");
    }

    private String formatPrice(long price) {
        if (price >= 1_000_000) {
            return String.format("%.1fM", price / 1_000_000.0);
        } else if (price >= 1000) {
            return String.format("%.1fK", price / 1000.0);
        }
        return String.valueOf(price);
    }

    private String formatCompactPrice(long price) {
        if (price >= 1_000_000) {
            return String.format("%.0fM", price / 1_000_000.0);
        } else if (price >= 1000) {
            return String.format("%.0fK", price / 1000.0);
        }
        return String.valueOf(price);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class ShopEntry {
        String itemId;
        String displayName;
        long price;
        String currency;
        String category;
        ItemStack cachedStack;
        ItemStack currencyStack;

        static ShopEntry fromJson(JsonObject json) {
            ShopEntry entry = new ShopEntry();
            entry.itemId = json.has("item") ? json.get("item").getAsString() : "";
            entry.price = json.has("price") ? json.get("price").getAsLong() : 0;
            entry.currency = json.has("currency") ? json.get("currency").getAsString() : "cobblecoins:bank";
            entry.category = json.has("category") ? json.get("category").getAsString() : "misc";
            
            // Try to get display name from item registry
            try {
                ResourceLocation itemLoc = ResourceLocation.parse(entry.itemId);
                Item item = BuiltInRegistries.ITEM.get(itemLoc);
                entry.displayName = item.getDescription().getString();
                entry.cachedStack = new ItemStack(item);
            } catch (Exception e) {
                entry.displayName = entry.itemId;
                entry.cachedStack = new ItemStack(Items.BARRIER);
            }
            
            // Cache currency item stack for display
            if (!entry.usesBankBalance()) {
                try {
                    ResourceLocation currencyLoc = ResourceLocation.parse(entry.currency);
                    Item currencyItem = BuiltInRegistries.ITEM.get(currencyLoc);
                    entry.currencyStack = new ItemStack(currencyItem);
                } catch (Exception e) {
                    entry.currencyStack = null;
                }
            }
            
            return entry;
        }

        String getDisplayName() {
            return displayName != null ? displayName : itemId;
        }

        ItemStack getItemStack() {
            return cachedStack != null ? cachedStack : new ItemStack(Items.BARRIER);
        }
        
        boolean usesBankBalance() {
            return currency == null || currency.isEmpty() || currency.equals("cobblecoins:bank");
        }
        
        String getCurrencyDisplayName() {
            if (usesBankBalance()) {
                return "CobbleCoins";
            }
            if (currencyStack != null && !currencyStack.isEmpty()) {
                return currencyStack.getItem().getDescription().getString();
            }
            return currency;
        }
        
        String getCurrencySymbol() {
            if (usesBankBalance()) {
                return "¢";
            }
            return getCurrencyDisplayName();
        }
    }
}
