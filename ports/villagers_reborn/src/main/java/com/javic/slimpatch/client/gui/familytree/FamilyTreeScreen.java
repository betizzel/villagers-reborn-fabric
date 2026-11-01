package com.javic.slimpatch.client.gui.familytree;

import com.javic.slimpatch.client.key.ModKeyBindings;
import com.javic.slimpatch.familytree.FamilyTreeEdgePayload;
import com.javic.slimpatch.familytree.FamilyTreeNodePayload;
import com.javic.slimpatch.network.CloseFamilyTreePacket;
import com.javic.slimpatch.network.ModNetworking;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;

public class FamilyTreeScreen extends Screen {
   private static final int WINDOW_WIDTH = 360;
   private static final int WINDOW_HEIGHT = 220;
   private static final int VIEWPORT_X = 9;
   private static final int VIEWPORT_Y = 18;
   private static final int NODE_WIDTH = 44;
   private static final int NODE_HEIGHT = 44;
   private static final int NODE_HORIZONTAL_GAP = 34;
   private static final int NODE_VERTICAL_GAP = 46;
   private static final int PORTRAIT_SIZE = 34;
   private static final int PORTRAIT_BORDER = 4;
   private static final int BASE_FAMILY_LINE_COLOR = -1644826;
   private static final int DESCENDANT_FAMILY_LINE_COLOR = -7350363;
   private static final int DEEP_DESCENDANT_FAMILY_LINE_COLOR = -13653937;
   private static final int DEEPEST_DESCENDANT_FAMILY_LINE_COLOR = -14714310;
   private static final int PLAYER_NODE_BORDER_COLOR = -9459457;
   private final UUID rootVillagerUuid;
   private final List<FamilyTreeNodePayload> nodes;
   private final List<FamilyTreeEdgePayload> edges;
   private final boolean truncated;
   private final Map<String, FamilyTreeNodePayload> nodesById = new LinkedHashMap<>();
   private final Map<String, FamilyTreeScreen.NodeLayout> layoutById = new HashMap<>();
   private final Map<String, ResourceLocation> portraitCache = new HashMap<>();
   private Map<String, Integer> lastComputedGenerations = Map.of();
   private double panX;
   private double panY;
   private boolean dragging;
   private int minContentX;
   private int minContentY;
   private int maxContentX;
   private int maxContentY;
   private boolean centered;
   private boolean closePacketSent;
   private boolean expandedView;

   public FamilyTreeScreen(UUID rootVillagerUuid, List<FamilyTreeNodePayload> nodes, List<FamilyTreeEdgePayload> edges, boolean truncated) {
      super(Component.translatable("slimpatch.screen.family_tree.title"));
      this.rootVillagerUuid = rootVillagerUuid;
      this.nodes = new ArrayList<>(nodes);
      this.edges = new ArrayList<>(edges);

      for (FamilyTreeNodePayload node : this.nodes) {
         this.nodesById.put(node.getNodeId(), node);
      }

      this.truncated = truncated;
   }

   protected void init() {
      super.init();
      this.computeLayout();
      this.centerOnRoot();
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) {
         return super.mouseClicked(mouseX, mouseY, button);
      }

      this.dragging = this.isMouseInsideViewport(mouseX, mouseY);
      return this.dragging || super.mouseClicked(mouseX, mouseY, button);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      if (button == 0) {
         this.dragging = false;
      }

      return super.mouseReleased(mouseX, mouseY, button);
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
      if (button == 0 && this.dragging) {
         this.panX += dragX;
         this.panY += dragY;
         this.clampPan();
         return true;
      } else {
         return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (keyCode == 70) {
         this.expandedView = !this.expandedView;
         this.clampPan();
         return true;
      } else if (ModKeyBindings.OPEN_DIALOGUE != null && ModKeyBindings.OPEN_DIALOGUE.matches(keyCode, scanCode)) {
         this.onClose();
         return true;
      } else {
         return super.keyPressed(keyCode, scanCode, modifiers);
      }
   }

   public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(graphics);
      int windowWidth = this.getWindowWidth();
      int windowHeight = this.getWindowHeight();
      int viewportWidth = this.getViewportWidth();
      int viewportHeight = this.getViewportHeight();
      int left = (this.width - windowWidth) / 2;
      int top = (this.height - windowHeight) / 2;
      graphics.fill(left, top, left + windowWidth, top + windowHeight, -267382768);
      graphics.fill(left + 1, top + 1, left + windowWidth - 1, top + windowHeight - 1, -533712848);
      graphics.fill(left + 9, top + 18, left + 9 + viewportWidth, top + 18 + viewportHeight, -15066598);
      graphics.drawCenteredString(this.font, this.title, left + windowWidth / 2, top + 6, 16777215);
      graphics.enableScissor(left + 9, top + 18, left + 9 + viewportWidth, top + 18 + viewportHeight);
      graphics.pose().pushPose();
      graphics.pose().translate((float)(left + 9 + this.panX), (float)(top + 18 + this.panY), 0.0F);
      this.renderEdges(graphics);
      this.renderNodes(graphics);
      graphics.pose().popPose();
      graphics.disableScissor();
      Component sizeHint = this.expandedView
         ? Component.translatable("slimpatch.screen.family_tree.normal_size_hint")
         : Component.translatable("slimpatch.screen.family_tree.expand_hint");
      graphics.drawString(this.font, sizeHint, left + 10, top + 6, 14211288, false);
      if (this.truncated) {
         graphics.drawString(this.font, Component.translatable("slimpatch.screen.family_tree.truncated"), left + 10, top + windowHeight - 10, 16765823, false);
      }

      this.renderNodeTooltip(graphics, mouseX, mouseY, left + 9, top + 18);
   }

   public boolean isPauseScreen() {
      return false;
   }

   public void onClose() {
      this.releaseViewHold();
      super.onClose();
   }

   public void removed() {
      this.releaseViewHold();
      super.removed();
   }

   private void renderEdges(GuiGraphics graphics) {
      Map<String, List<FamilyTreeEdgePayload>> parentEdgesByChild = new HashMap<>();
      Map<String, FamilyTreeScreen.FamilyBlock> familyBlocks = new LinkedHashMap<>();
      Map<String, FamilyTreeScreen.FamilyBlock> formerFamilyBlocks = new LinkedHashMap<>();
      Set<String> usedEdges = new HashSet<>();
      Set<String> blockedFallbackChildIds = new HashSet<>();

      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.PARENT_CHILD) {
            parentEdgesByChild.computeIfAbsent(edge.getToNodeId(), ignored -> new ArrayList<>()).add(edge);
         }
      }

      for (Entry<String, List<FamilyTreeEdgePayload>> entry : parentEdgesByChild.entrySet()) {
         String childNodeId = entry.getKey();
         FamilyTreeNodePayload childNode = this.nodesById.get(childNodeId);
         if (childNode != null && childNode.getNodeType() == FamilyTreeNodePayload.NodeType.PLAYER) {
            for (FamilyTreeEdgePayload edge : entry.getValue()) {
               usedEdges.add(this.edgeKey(edge));
            }
         } else {
            List<FamilyTreeEdgePayload> parentEdges = entry.getValue();
            List<String> parentIds = new ArrayList<>();

            for (FamilyTreeEdgePayload edge : parentEdges) {
               if (this.layoutById.containsKey(edge.getFromNodeId())) {
                  parentIds.add(edge.getFromNodeId());
               }
            }

            if (!parentIds.isEmpty()) {
               FamilyTreeEdgePayload spouseEdge = null;
               if (parentIds.size() < 2) {
                  spouseEdge = this.findSpouseEdge(parentIds.get(0), usedEdges);
                  if (spouseEdge != null) {
                     String spouseId = spouseEdge.getFromNodeId().equals(parentIds.get(0)) ? spouseEdge.getToNodeId() : spouseEdge.getFromNodeId();
                     if (this.layoutById.containsKey(spouseId)) {
                        parentIds.add(spouseId);
                     }
                  }
               }

               if (parentIds.size() >= 2) {
                  parentIds.sort(this::compareNodeIds);
                  if (parentIds.size() != 2 || !this.isFormerSpousePair(parentIds.get(0), parentIds.get(1))) {
                     String blockKey = String.join("|", parentIds);
                     FamilyTreeScreen.FamilyBlock block = familyBlocks.computeIfAbsent(blockKey, ignored -> new FamilyTreeScreen.FamilyBlock(parentIds));
                     if (!block.childNodeIds().contains(childNodeId)) {
                        block.childNodeIds().add(childNodeId);
                     }

                     for (FamilyTreeEdgePayload edge : parentEdges) {
                        usedEdges.add(this.edgeKey(edge));
                     }

                     if (spouseEdge != null) {
                        usedEdges.add(this.edgeKey(spouseEdge));
                     }
                  }
               }
            }
         }
      }

      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.FORMER_SPOUSE) {
            List<String> sharedChildren = this.findSharedChildren(edge.getFromNodeId(), edge.getToNodeId());
            if (!sharedChildren.isEmpty()) {
               List<String> parentIds = new ArrayList<>();
               parentIds.add(edge.getFromNodeId());
               parentIds.add(edge.getToNodeId());
               parentIds.sort(this::compareNodeIds);
               FamilyTreeScreen.FamilyBlock block = new FamilyTreeScreen.FamilyBlock(parentIds);
               block.childNodeIds().addAll(sharedChildren);
               formerFamilyBlocks.put(String.join("|", parentIds), block);
               usedEdges.add(this.edgeKey(edge));
               blockedFallbackChildIds.addAll(sharedChildren);

               for (String childId : sharedChildren) {
                  for (FamilyTreeEdgePayload childEdge : this.edges) {
                     if (childEdge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.PARENT_CHILD
                        && this.isParentChildLinkForNodes(childEdge, childId, edge.getFromNodeId(), edge.getToNodeId())) {
                        usedEdges.add(this.edgeKey(childEdge));
                     }
                  }
               }
            }
         }
      }

      int baseFamilyGeneration = this.getBaseFamilyGeneration(familyBlocks.values());

      for (FamilyTreeScreen.FamilyBlock block : familyBlocks.values()) {
         block.childNodeIds().sort(this::compareNodeIds);
         blockedFallbackChildIds.addAll(block.childNodeIds());
         List<FamilyTreeScreen.NodeLayout> parentLayouts = new ArrayList<>();

         for (String parentId : block.parentNodeIds()) {
            FamilyTreeScreen.NodeLayout layout = this.layoutById.get(parentId);
            if (layout != null) {
               parentLayouts.add(layout);
            }
         }

         List<FamilyTreeScreen.NodeLayout> childLayouts = new ArrayList<>();

         for (String childId : block.childNodeIds()) {
            FamilyTreeScreen.NodeLayout layout = this.layoutById.get(childId);
            if (layout != null) {
               childLayouts.add(layout);
            }
         }

         if (parentLayouts.size() >= 2 && !childLayouts.isEmpty()) {
            parentLayouts.sort(Comparator.comparingInt(layout -> layout.x));
            childLayouts.sort(Comparator.comparingInt(layout -> layout.x));
            this.markSpouseEdgesAsUsed(block, usedEdges);
            int blockColor = this.getFamilyBlockLineColor(block, baseFamilyGeneration);
            int leftParentX = parentLayouts.get(0).x + 22;
            int rightParentX = parentLayouts.get(parentLayouts.size() - 1).x + 22;
            int parentCenterY = parentLayouts.get(0).y + 22;
            int coupleCenterX = (leftParentX + rightParentX) / 2;
            drawFamilyLine(graphics, leftParentX, parentCenterY, rightParentX, parentCenterY, blockColor);
            if (childLayouts.size() == 1) {
               int childCenterX = childLayouts.get(0).x + 22;
               int childCenterY = childLayouts.get(0).y + 22;
               if (childCenterX == coupleCenterX) {
                  drawFamilyLine(graphics, coupleCenterX, parentCenterY, coupleCenterX, childCenterY, blockColor);
               } else {
                  int connectorY = parentCenterY + Math.max(10, (childCenterY - parentCenterY) / 2);
                  drawFamilyLine(graphics, coupleCenterX, parentCenterY, coupleCenterX, connectorY, blockColor);
                  drawFamilyLine(graphics, Math.min(coupleCenterX, childCenterX), connectorY, Math.max(coupleCenterX, childCenterX), connectorY, blockColor);
                  drawFamilyLine(graphics, childCenterX, connectorY, childCenterX, childCenterY, blockColor);
               }
            } else if (childLayouts.size() != 2) {
               int firstChildX = childLayouts.get(0).x + 22;
               int lastChildX = childLayouts.get(childLayouts.size() - 1).x + 22;
               int childCenterY = childLayouts.get(0).y + 22;
               drawFamilyLine(graphics, coupleCenterX, parentCenterY, coupleCenterX, childCenterY, blockColor);
               drawFamilyLine(graphics, firstChildX, childCenterY, lastChildX, childCenterY, blockColor);
            } else {
               for (String childId : block.childNodeIds()) {
                  for (FamilyTreeEdgePayload edge : this.edges) {
                     if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.PARENT_CHILD && edge.getToNodeId().equals(childId)) {
                        usedEdges.add(this.edgeKey(edge));
                     }
                  }
               }

               int firstChildX = childLayouts.get(0).x + 22;
               int secondChildX = childLayouts.get(1).x + 22;
               int firstChildCenterY = childLayouts.get(0).y + 22;
               int secondChildCenterY = childLayouts.get(1).y + 22;
               int portraitTopY = childLayouts.get(0).y + 5;
               int barY = portraitTopY - 12;
               drawFamilyLine(graphics, coupleCenterX, parentCenterY, coupleCenterX, barY, blockColor);
               drawFamilyLine(graphics, firstChildX, barY, secondChildX, barY, blockColor);
               drawFamilyLine(graphics, firstChildX, barY, firstChildX, firstChildCenterY, blockColor);
               drawFamilyLine(graphics, secondChildX, barY, secondChildX, secondChildCenterY, blockColor);
            }
         }
      }

      for (FamilyTreeScreen.FamilyBlock block : formerFamilyBlocks.values()) {
         List<FamilyTreeScreen.NodeLayout> parentLayouts = new ArrayList<>();

         for (String parentId : block.parentNodeIds()) {
            FamilyTreeScreen.NodeLayout layout = this.layoutById.get(parentId);
            if (layout != null) {
               parentLayouts.add(layout);
            }
         }

         List<FamilyTreeScreen.NodeLayout> childLayouts = new ArrayList<>();

         for (String childId : block.childNodeIds()) {
            FamilyTreeScreen.NodeLayout layout = this.layoutById.get(childId);
            if (layout != null) {
               childLayouts.add(layout);
            }
         }

         if (parentLayouts.size() >= 2 && !childLayouts.isEmpty()) {
            parentLayouts.sort(Comparator.comparingInt(layout -> layout.x));
            childLayouts.sort(Comparator.comparingInt(layout -> layout.x));
            int leftParentX = parentLayouts.get(0).x + 22;
            int rightParentX = parentLayouts.get(parentLayouts.size() - 1).x + 22;
            int parentCenterY = parentLayouts.get(0).y + 22;
            int childLineY = childLayouts.get(0).y + 22;
            int firstChildX = childLayouts.get(0).x + 22;
            int lastChildX = childLayouts.get(childLayouts.size() - 1).x + 22;
            drawFamilyLine(graphics, leftParentX, parentCenterY, leftParentX, childLineY);
            drawFamilyLine(graphics, rightParentX, parentCenterY, rightParentX, childLineY);
            drawFamilyLine(graphics, Math.min(leftParentX, firstChildX), childLineY, Math.max(rightParentX, lastChildX), childLineY);
         }
      }

      for (FamilyTreeEdgePayload edge : this.edges) {
         if (!usedEdges.contains(this.edgeKey(edge))
            && (edge.getEdgeType() != FamilyTreeEdgePayload.EdgeType.PARENT_CHILD || !blockedFallbackChildIds.contains(edge.getToNodeId()))) {
            FamilyTreeScreen.NodeLayout from = this.layoutById.get(edge.getFromNodeId());
            FamilyTreeScreen.NodeLayout to = this.layoutById.get(edge.getToNodeId());
            if (from != null && to != null) {
               int fromX = from.x + 22;
               int fromY = from.y + 22;
               int toX = to.x + 22;
               int toY = to.y + 22;
               if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.SPOUSE) {
                  drawFamilyLine(graphics, fromX, fromY, toX, toY, this.getSpouseLineColor(edge, baseFamilyGeneration));
               } else if (fromX == toX || fromY == toY) {
                  drawFamilyLine(graphics, fromX, fromY, toX, toY);
               }
            }
         }
      }
   }

   private void renderNodes(GuiGraphics graphics) {
      for (FamilyTreeNodePayload node : this.nodes) {
         FamilyTreeScreen.NodeLayout layout = this.layoutById.get(node.getNodeId());
         if (layout != null) {
            this.renderPortrait(graphics, node, layout.x + 5, layout.y + 5);
         }
      }
   }

   private void renderPortrait(GuiGraphics graphics, FamilyTreeNodePayload node, int x, int y) {
      int borderColor = this.getPortraitBorder(node);
      this.drawRoundedPortraitFrame(graphics, x, y, borderColor);
      if (node.isPlaceholder()) {
         this.renderPlaceholderPortrait(graphics, node, x, y, "?");
      } else {
         ResourceLocation portraitTexture = FamilyTreePortraitResolver.resolve(Minecraft.getInstance(), node, this.portraitCache);
         if (portraitTexture == null) {
            if (node.getNodeType() == FamilyTreeNodePayload.NodeType.PLAYER) {
               String marker = node.getDisplayName().isEmpty() ? "P" : node.getDisplayName().substring(0, 1).toUpperCase(Locale.ROOT);
               this.renderPlaceholderPortrait(graphics, node, x, y, marker);
            } else {
               this.renderPlaceholderPortrait(
                  graphics, node, x, y, node.getGender().isEmpty() ? "?" : node.getGender().substring(0, 1).toUpperCase(Locale.ROOT)
               );
            }
         } else {
            graphics.blit(portraitTexture, x, y, 34, 34, 8.0F, 8.0F, 8, 8, 64, 64);
            graphics.blit(portraitTexture, x, y, 34, 34, 40.0F, 8.0F, 8, 8, 64, 64);
            if (!node.isAlive() || node.getPortrait().isDeadGraySuggested()) {
               graphics.fill(x, y, x + 34, y + 34, -867546550);
            }

            this.drawPortraitCornerMask(graphics, x, y);
         }
      }
   }

   private void renderPlaceholderPortrait(GuiGraphics graphics, FamilyTreeNodePayload node, int x, int y, String marker) {
      int background = node.getNodeType() == FamilyTreeNodePayload.NodeType.PLAYER ? -12691850 : -11776948;
      if (node.isPlaceholder()) {
         background = -13290187;
      }

      if (!node.isAlive()) {
         background = -10855846;
      }

      graphics.fill(x, y, x + 34, y + 34, background);
      this.drawBoldCenteredString(graphics, marker, x + 17, y + (34 - 9) / 2, -1);
      this.drawPortraitCornerMask(graphics, x, y);
   }

   private void drawRoundedPortraitFrame(GuiGraphics graphics, int x, int y, int borderColor) {
      this.drawRoundedRect(graphics, x - 4, y - 4, 42, -16250872, 4);
      this.drawRoundedRect(graphics, x - 4 + 1, y - 4 + 1, 40, borderColor, 4);
      this.drawRoundedRect(graphics, x - 1, y - 1, 36, -15724528, 3);
   }

   private void drawRoundedRect(GuiGraphics graphics, int x, int y, int size, int color, int radius) {
      int right = x + size;
      int bottom = y + size;
      graphics.fill(x + radius, y, right - radius, bottom, color);
      graphics.fill(x, y + radius, right, bottom - radius, color);
      graphics.fill(x + 1, y + 1, x + radius, y + radius, color);
      graphics.fill(right - radius, y + 1, right - 1, y + radius, color);
      graphics.fill(x + 1, bottom - radius, x + radius, bottom - 1, color);
      graphics.fill(right - radius, bottom - radius, right - 1, bottom - 1, color);
   }

   private void renderNodeTooltip(GuiGraphics graphics, int mouseX, int mouseY, int viewportLeft, int viewportTop) {
      FamilyTreeNodePayload hovered = this.getHoveredNode(mouseX, mouseY, viewportLeft, viewportTop);
      if (hovered != null) {
         List<Component> tooltip = new ArrayList<>();
         tooltip.add(Component.literal(this.getNodeDisplayName(hovered.getNodeId())).withStyle(ChatFormatting.GOLD));
         if (hovered.isPlaceholder()) {
            tooltip.add(Component.translatable("slimpatch.screen.family_tree.unknown_member"));
         } else {
            tooltip.add(
               (hovered.getNodeType() == FamilyTreeNodePayload.NodeType.PLAYER
                     ? Component.translatable("slimpatch.screen.family_tree.player")
                     : Component.translatable("slimpatch.screen.family_tree.villager"))
                  .withStyle(ChatFormatting.GRAY)
            );
            if (hovered.getNodeType() == FamilyTreeNodePayload.NodeType.VILLAGER) {
               tooltip.add(Component.literal(this.resolveLifeStage(hovered.getAgeStage())).withStyle(ChatFormatting.GRAY));
               if (!hovered.isAlive()) {
                  tooltip.add(Component.translatable("slimpatch.screen.family_tree.deceased").withStyle(ChatFormatting.RED));
               }
            }
         }

         String relation = this.getRelationToVisiblePlayer(hovered.getNodeId());
         if (relation != null && !relation.isEmpty()) {
            tooltip.add(Component.literal("Relation: " + relation).withStyle(ChatFormatting.GREEN));
         }

         String parents = this.formatLimitedNameList(this.getParentIds(hovered.getNodeId()), 2, false);
         if (!parents.isEmpty()) {
            tooltip.add(Component.literal(Component.translatable("slimpatch.screen.family_tree.parents").getString() + ": " + parents));
         }

         String siblings = this.formatLimitedNameList(this.getSiblingIds(hovered.getNodeId()), 5, true);
         if (!siblings.isEmpty()) {
            tooltip.add(Component.literal(Component.translatable("slimpatch.screen.family_tree.siblings").getString() + ": " + siblings));
         }

         String spouse = this.formatLimitedNameList(this.getSpouseIds(hovered.getNodeId()), 1, false);
         if (!spouse.isEmpty()) {
            tooltip.add(Component.literal(Component.translatable("slimpatch.screen.family_tree.spouse").getString() + ": " + spouse));
         }

         String children = this.formatLimitedNameList(this.getChildIds(hovered.getNodeId()), 5, true);
         if (!children.isEmpty()) {
            tooltip.add(Component.literal(Component.translatable("slimpatch.screen.family_tree.children").getString() + ": " + children));
         }

         List<FormattedCharSequence> lines = new ArrayList<>(tooltip.size());

         for (Component component : tooltip) {
            lines.add(component.getVisualOrderText());
         }

         graphics.renderTooltip(this.font, lines, mouseX, mouseY);
      }
   }

   private FamilyTreeNodePayload getHoveredNode(int mouseX, int mouseY, int viewportLeft, int viewportTop) {
      double contentX = mouseX - viewportLeft - this.panX;
      double contentY = mouseY - viewportTop - this.panY;

      for (FamilyTreeNodePayload node : this.nodes) {
         FamilyTreeScreen.NodeLayout layout = this.layoutById.get(node.getNodeId());
         if (layout != null && contentX >= layout.x && contentX <= layout.x + 44 && contentY >= layout.y && contentY <= layout.y + 44) {
            return node;
         }
      }

      return null;
   }

   private void computeLayout() {
      this.layoutById.clear();
      Map<String, Integer> generations = this.computeGenerations();
      this.lastComputedGenerations = new HashMap<>(generations);
      FamilyTreeScreen.LayoutContext context = this.buildLayoutContext(generations);
      List<Integer> orderedGenerations = new ArrayList<>(context.nodeIdsByGeneration().keySet());
      orderedGenerations.sort(Integer::compareTo);
      this.resetBounds();

      for (int generation : orderedGenerations) {
         List<FamilyTreeScreen.FamilyUnit> units = this.buildGenerationUnits(generation, context);
         this.layoutGenerationUnits(units, generation, context);
      }

      this.finalizeEmptyBounds();
   }

   private FamilyTreeScreen.LayoutContext buildLayoutContext(Map<String, Integer> generations) {
      Map<String, List<String>> spouseIdsByNode = new HashMap<>();
      Map<String, List<String>> partnerIdsByNode = new HashMap<>();
      Map<String, List<String>> parentIdsByChild = new HashMap<>();
      Map<String, List<String>> childIdsByFamilyKey = new HashMap<>();
      Map<String, List<String>> siblingIdsByNode = new HashMap<>();
      Map<Integer, List<String>> nodeIdsByGeneration = new LinkedHashMap<>();
      Map<Integer, Set<String>> prelaidOutNodeIdsByGeneration = new HashMap<>();

      for (FamilyTreeNodePayload node : this.nodes) {
         int generation = generations.getOrDefault(node.getNodeId(), 0);
         nodeIdsByGeneration.computeIfAbsent(generation, ignored -> new ArrayList<>()).add(node.getNodeId());
      }

      for (List<String> nodeIds : nodeIdsByGeneration.values()) {
         nodeIds.sort(this::compareNodeIds);
      }

      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.SPOUSE) {
            this.addRelatedNode(spouseIdsByNode, edge.getFromNodeId(), edge.getToNodeId());
            this.addRelatedNode(spouseIdsByNode, edge.getToNodeId(), edge.getFromNodeId());
            this.addRelatedNode(partnerIdsByNode, edge.getFromNodeId(), edge.getToNodeId());
            this.addRelatedNode(partnerIdsByNode, edge.getToNodeId(), edge.getFromNodeId());
         } else if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.FORMER_SPOUSE
            && !this.findSharedChildren(edge.getFromNodeId(), edge.getToNodeId()).isEmpty()) {
            this.addRelatedNode(partnerIdsByNode, edge.getFromNodeId(), edge.getToNodeId());
            this.addRelatedNode(partnerIdsByNode, edge.getToNodeId(), edge.getFromNodeId());
         }
      }

      for (FamilyTreeNodePayload node : this.nodes) {
         List<String> parentIds = this.getParentIds(node.getNodeId(), generations);
         if (!parentIds.isEmpty()) {
            parentIdsByChild.put(node.getNodeId(), parentIds);
         }
      }

      for (Entry<String, List<String>> entry : parentIdsByChild.entrySet()) {
         List<String> parentIds = this.resolveFamilyParentIds(entry.getKey(), generations, spouseIdsByNode);
         if (parentIds.size() >= 2) {
            String familyKey = this.toFamilyKey(parentIds.get(0), parentIds.get(1));
            childIdsByFamilyKey.computeIfAbsent(familyKey, ignored -> new ArrayList<>()).add(entry.getKey());
         }
      }

      for (List<String> childIds : childIdsByFamilyKey.values()) {
         childIds.sort(this::compareNodeIds);

         for (String childId : childIds) {
            siblingIdsByNode.put(childId, childIds);
         }
      }

      return new FamilyTreeScreen.LayoutContext(
         generations,
         spouseIdsByNode,
         partnerIdsByNode,
         parentIdsByChild,
         childIdsByFamilyKey,
         siblingIdsByNode,
         nodeIdsByGeneration,
         prelaidOutNodeIdsByGeneration
      );
   }

   private List<String> resolveFamilyParentIds(String childNodeId, Map<String, Integer> generations, Map<String, List<String>> spouseIdsByNode) {
      List<String> parentIds = this.getParentIds(childNodeId);
      if (parentIds.size() >= 2) {
         return parentIds;
      }

      if (parentIds.size() != 1) {
         return parentIds;
      }

      String spouseId = this.getVisibleSpouseId(parentIds.get(0), generations, spouseIdsByNode);
      if (spouseId == null) {
         return parentIds;
      }

      List<String> resolvedParentIds = new ArrayList<>(parentIds);
      resolvedParentIds.add(spouseId);
      resolvedParentIds.sort(this::compareNodeIds);
      return resolvedParentIds;
   }

   private List<FamilyTreeScreen.FamilyUnit> buildGenerationUnits(int generation, FamilyTreeScreen.LayoutContext context) {
      List<String> orderedNodeIds = this.orderGenerationNodeIds(generation, context);
      Set<String> consumedNodeIds = new HashSet<>();
      List<FamilyTreeScreen.FamilyUnit> units = new ArrayList<>();

      for (String nodeId : orderedNodeIds) {
         if (!consumedNodeIds.contains(nodeId) && !this.layoutById.containsKey(nodeId)) {
            FamilyTreeScreen.FamilyUnit unit = this.buildFamilyUnit(nodeId, generation, context, new HashSet<>());
            units.add(unit);
            consumedNodeIds.add(nodeId);
            if (unit.spouseNodeId() != null && generation == context.generations().getOrDefault(unit.spouseNodeId(), Integer.MIN_VALUE)) {
               consumedNodeIds.add(unit.spouseNodeId());
            }
         }
      }

      return units;
   }

   private List<String> orderGenerationNodeIds(int generation, FamilyTreeScreen.LayoutContext context) {
      List<String> orderedNodeIds = new ArrayList<>(context.nodeIdsByGeneration().getOrDefault(generation, List.of()));
      Set<String> consumedNodeIds = new HashSet<>();
      List<String> reorderedNodeIds = new ArrayList<>();

      for (String nodeId : orderedNodeIds) {
         if (consumedNodeIds.add(nodeId)) {
            String coparentNodeId = this.findVisibleCoparentPair(nodeId, generation, context, consumedNodeIds);
            if (coparentNodeId != null) {
               reorderedNodeIds.add(nodeId);
               reorderedNodeIds.add(coparentNodeId);
               consumedNodeIds.add(coparentNodeId);
            } else {
               reorderedNodeIds.add(nodeId);
            }
         }
      }

      return reorderedNodeIds;
   }

   private String findVisibleCoparentPair(String nodeId, int generation, FamilyTreeScreen.LayoutContext context, Set<String> consumedNodeIds) {
      String selectedPairNodeId = null;

      for (FamilyTreeNodePayload node : this.nodes) {
         if (node.getNodeType() != FamilyTreeNodePayload.NodeType.PLAYER
            && context.generations().getOrDefault(node.getNodeId(), Integer.MIN_VALUE) == generation + 1) {
            List<String> parentIds = this.getParentIds(node.getNodeId());
            if (parentIds.size() == 2 && parentIds.contains(nodeId)) {
               String otherParentId = parentIds.get(0).equals(nodeId) ? parentIds.get(1) : parentIds.get(0);
               if (context.generations().getOrDefault(otherParentId, Integer.MIN_VALUE) == generation
                  && !consumedNodeIds.contains(otherParentId)
                  && (this.layoutById.isEmpty() || !this.layoutById.containsKey(otherParentId))
                  && (selectedPairNodeId == null || this.compareNodeIds(otherParentId, selectedPairNodeId) < 0)) {
                  selectedPairNodeId = otherParentId;
               }
            }
         }
      }

      return selectedPairNodeId;
   }

   private FamilyTreeScreen.FamilyUnit buildFamilyUnit(String nodeId, int generation, FamilyTreeScreen.LayoutContext context, Set<String> path) {
      FamilyTreeNodePayload node = this.nodesById.get(nodeId);
      if (node != null && path.add(nodeId)) {
         String spouseNodeId = this.getVisibleSpouseId(nodeId, context);
         boolean spouseAvailable = spouseNodeId != null
            && generation == context.generations().getOrDefault(spouseNodeId, Integer.MIN_VALUE)
            && !this.layoutById.containsKey(spouseNodeId)
            && !path.contains(spouseNodeId);
         if (spouseAvailable && this.isEligibleDescendantFamilyStarter(node, context)) {
            List<String> childNodeIds = this.getVisibleChildrenForPair(nodeId, spouseNodeId, context);
            if (!childNodeIds.isEmpty()) {
               List<FamilyTreeScreen.FamilyUnit> childUnits = new ArrayList<>();
               int childGeneration = context.generations().getOrDefault(nodeId, generation) + 1;

               for (String childNodeId : childNodeIds) {
                  childUnits.add(this.buildFamilyUnit(childNodeId, childGeneration, context, path));
               }

               FamilyTreeScreen.SpouseSide spouseSide = this.decideSpouseSide(nodeId, context.siblingIdsByNode().getOrDefault(nodeId, List.of()), context);
               FamilyTreeScreen.FamilyUnit unit = new FamilyTreeScreen.FamilyUnit(
                  nodeId, spouseNodeId, childNodeIds, childUnits, FamilyTreeScreen.UnitType.DESCENDANT_FAMILY, spouseSide
               );
               path.remove(nodeId);
               return unit;
            }

            if (context.parentIdsByChild().containsKey(nodeId)) {
               FamilyTreeScreen.SpouseSide spouseSide = this.decideSpouseSide(nodeId, context.siblingIdsByNode().getOrDefault(nodeId, List.of()), context);
               FamilyTreeScreen.FamilyUnit unit = new FamilyTreeScreen.FamilyUnit(
                  nodeId, spouseNodeId, List.of(), List.of(), FamilyTreeScreen.UnitType.COUPLE, spouseSide
               );
               path.remove(nodeId);
               return unit;
            }
         }

         FamilyTreeScreen.FamilyUnit unit = new FamilyTreeScreen.FamilyUnit(
            nodeId, null, List.of(), List.of(), FamilyTreeScreen.UnitType.SINGLE, FamilyTreeScreen.SpouseSide.RIGHT
         );
         path.remove(nodeId);
         return unit;
      } else {
         return new FamilyTreeScreen.FamilyUnit(nodeId, null, List.of(), List.of(), FamilyTreeScreen.UnitType.SINGLE, FamilyTreeScreen.SpouseSide.RIGHT);
      }
   }

   private int computeUnitWidth(FamilyTreeScreen.FamilyUnit unit, FamilyTreeScreen.LayoutContext context) {
      if (unit.unitType() == FamilyTreeScreen.UnitType.SINGLE) {
         return 44;
      }

      int coupleWidth = 122;
      if (unit.unitType() == FamilyTreeScreen.UnitType.COUPLE) {
         return coupleWidth;
      }

      int childrenWidth = 0;

      for (FamilyTreeScreen.FamilyUnit childUnit : unit.childUnits()) {
         childrenWidth += this.computeUnitWidth(childUnit, context);
      }

      childrenWidth += Math.max(0, unit.childUnits().size() - 1) * 34;
      return Math.max(coupleWidth, childrenWidth);
   }

   private boolean isEligibleDescendantFamilyStarter(FamilyTreeNodePayload node, FamilyTreeScreen.LayoutContext context) {
      if (node == null || node.getNodeType() != FamilyTreeNodePayload.NodeType.VILLAGER) {
         return false;
      } else {
         return !"ADULT".equals(node.getAgeStage())
            ? false
            : context.parentIdsByChild().containsKey(node.getNodeId()) && this.getVisibleSpouseId(node.getNodeId(), context) != null;
      }
   }

   private String getVisibleSpouseId(String nodeId, FamilyTreeScreen.LayoutContext context) {
      List<String> spouseIds = context.partnerIdsByNode().get(nodeId);
      if (spouseIds != null && !spouseIds.isEmpty()) {
         String selectedSpouseId = null;
         int generation = context.generations().getOrDefault(nodeId, 0);

         for (String spouseId : spouseIds) {
            if (generation == context.generations().getOrDefault(spouseId, Integer.MIN_VALUE)
               && (selectedSpouseId == null || this.compareNodeIds(spouseId, selectedSpouseId) < 0)) {
               selectedSpouseId = spouseId;
            }
         }

         return selectedSpouseId;
      } else {
         return null;
      }
   }

   private String getVisibleSpouseId(String nodeId, Map<String, Integer> generations, Map<String, List<String>> spouseIdsByNode) {
      List<String> spouseIds = spouseIdsByNode.get(nodeId);
      if (spouseIds != null && !spouseIds.isEmpty()) {
         String selectedSpouseId = null;
         int generation = generations.getOrDefault(nodeId, 0);

         for (String spouseId : spouseIds) {
            if (generation == generations.getOrDefault(spouseId, Integer.MIN_VALUE)
               && (selectedSpouseId == null || this.compareNodeIds(spouseId, selectedSpouseId) < 0)) {
               selectedSpouseId = spouseId;
            }
         }

         return selectedSpouseId;
      } else {
         return null;
      }
   }

   private List<String> getVisibleChildrenForPair(String firstParentId, String secondParentId, FamilyTreeScreen.LayoutContext context) {
      String familyKey = this.toFamilyKey(firstParentId, secondParentId);
      List<String> childIds = new ArrayList<>(context.childIdsByFamilyKey().getOrDefault(familyKey, List.of()));
      int expectedGeneration = context.generations().getOrDefault(firstParentId, 0) + 1;
      childIds.removeIf(childId -> context.generations().getOrDefault(childId, Integer.MIN_VALUE) != expectedGeneration);
      childIds.sort(this::compareNodeIds);
      return childIds;
   }

   private FamilyTreeScreen.SpouseSide decideSpouseSide(String nodeId, List<String> siblingIds, FamilyTreeScreen.LayoutContext context) {
      if (siblingIds != null && !siblingIds.isEmpty()) {
         int index = siblingIds.indexOf(nodeId);
         if (index < 0) {
            return FamilyTreeScreen.SpouseSide.RIGHT;
         }

         double middle = (siblingIds.size() - 1) / 2.0;
         return index < middle ? FamilyTreeScreen.SpouseSide.LEFT : FamilyTreeScreen.SpouseSide.RIGHT;
      } else {
         return FamilyTreeScreen.SpouseSide.RIGHT;
      }
   }

   private void layoutGenerationUnits(List<FamilyTreeScreen.FamilyUnit> units, int generation, FamilyTreeScreen.LayoutContext context) {
      if (!units.isEmpty()) {
         int totalWidth = 0;

         for (FamilyTreeScreen.FamilyUnit unit : units) {
            totalWidth += this.computeUnitWidth(unit, context);
         }

         totalWidth += Math.max(0, units.size() - 1) * 34;
         int currentX = -totalWidth / 2;
         int generationY = this.getGenerationY(generation);

         for (FamilyTreeScreen.FamilyUnit unit : units) {
            int unitWidth = this.computeUnitWidth(unit, context);
            int centerX = currentX + unitWidth / 2;
            this.placeUnit(unit, centerX, generationY, context);
            currentX += unitWidth + 34;
         }
      }
   }

   private void placeUnit(FamilyTreeScreen.FamilyUnit unit, int centerX, int generationY, FamilyTreeScreen.LayoutContext context) {
      if (unit.unitType() == FamilyTreeScreen.UnitType.SINGLE) {
         this.placeSimpleNode(unit.anchorNodeId(), centerX, generationY);
      } else if (unit.unitType() == FamilyTreeScreen.UnitType.COUPLE) {
         this.placeCouple(unit.anchorNodeId(), unit.spouseNodeId(), unit.spouseSide(), centerX, generationY);
      } else {
         this.placeDescendantFamily(unit, centerX, generationY, context);
      }
   }

   private void placeSimpleNode(String nodeId, int centerX, int generationY) {
      int x = centerX - 22;
      this.layoutById.put(nodeId, new FamilyTreeScreen.NodeLayout(x, generationY));
      this.updateBounds(nodeId);
   }

   private void placeCouple(String primaryNodeId, String spouseNodeId, FamilyTreeScreen.SpouseSide side, int centerX, int generationY) {
      int coupleWidth = 122;
      int leftX = centerX - coupleWidth / 2;
      int rightX = leftX + 44 + 34;
      if (side == FamilyTreeScreen.SpouseSide.LEFT) {
         this.layoutById.put(spouseNodeId, new FamilyTreeScreen.NodeLayout(leftX, generationY));
         this.layoutById.put(primaryNodeId, new FamilyTreeScreen.NodeLayout(rightX, generationY));
         this.updateBounds(spouseNodeId);
         this.updateBounds(primaryNodeId);
      } else {
         this.layoutById.put(primaryNodeId, new FamilyTreeScreen.NodeLayout(leftX, generationY));
         this.layoutById.put(spouseNodeId, new FamilyTreeScreen.NodeLayout(rightX, generationY));
         this.updateBounds(primaryNodeId);
         this.updateBounds(spouseNodeId);
      }
   }

   private void placeDescendantFamily(FamilyTreeScreen.FamilyUnit unit, int centerX, int generationY, FamilyTreeScreen.LayoutContext context) {
      this.placeCouple(unit.anchorNodeId(), unit.spouseNodeId(), unit.spouseSide(), centerX, generationY);
      if (!unit.childUnits().isEmpty()) {
         int childGeneration = context.generations().getOrDefault(unit.anchorNodeId(), 0) + 1;
         int childY = this.getGenerationY(childGeneration);

         for (FamilyTreeScreen.ChildPlacement childPlacement : this.computeChildPlacements(unit.childUnits(), centerX, context)) {
            this.placeUnit(childPlacement.unit(), childPlacement.centerX(), childY, context);
         }
      }
   }

   private List<FamilyTreeScreen.ChildPlacement> computeChildPlacements(
      List<FamilyTreeScreen.FamilyUnit> childUnits, int familyCenterX, FamilyTreeScreen.LayoutContext context
   ) {
      List<FamilyTreeScreen.ChildPlacement> placements = new ArrayList<>();
      if (childUnits.isEmpty()) {
         return placements;
      }

      int childrenWidth = 0;

      for (FamilyTreeScreen.FamilyUnit childUnit : childUnits) {
         childrenWidth += this.computeUnitWidth(childUnit, context);
      }

      childrenWidth += Math.max(0, childUnits.size() - 1) * 34;
      int startX = familyCenterX - childrenWidth / 2;
      int currentX = startX;

      for (FamilyTreeScreen.FamilyUnit childUnit : childUnits) {
         int unitWidth = this.computeUnitWidth(childUnit, context);
         int centerX = currentX + unitWidth / 2;
         placements.add(new FamilyTreeScreen.ChildPlacement(childUnit, centerX));
         currentX += unitWidth + 34;
      }

      return placements;
   }

   private int getGenerationY(int generation) {
      return generation * 90;
   }

   private void resetBounds() {
      this.minContentX = Integer.MAX_VALUE;
      this.minContentY = Integer.MAX_VALUE;
      this.maxContentX = Integer.MIN_VALUE;
      this.maxContentY = Integer.MIN_VALUE;
   }

   private void updateBounds(String nodeId) {
      FamilyTreeScreen.NodeLayout layout = this.layoutById.get(nodeId);
      if (layout != null) {
         this.minContentX = Math.min(this.minContentX, layout.x);
         this.minContentY = Math.min(this.minContentY, layout.y);
         this.maxContentX = Math.max(this.maxContentX, layout.x + 44);
         this.maxContentY = Math.max(this.maxContentY, layout.y + 44);
      }
   }

   private void finalizeEmptyBounds() {
      if (this.layoutById.isEmpty()) {
         this.minContentX = 0;
         this.minContentY = 0;
         this.maxContentX = 44;
         this.maxContentY = 44;
      }
   }

   private String toFamilyKey(String firstParentId, String secondParentId) {
      return this.compareNodeIds(firstParentId, secondParentId) <= 0 ? firstParentId + "|" + secondParentId : secondParentId + "|" + firstParentId;
   }

   private Map<String, Integer> computeGenerations() {
      Map<String, Integer> generations = new HashMap<>();
      String anchorNodeId = this.findAnchorNodeId();
      if (anchorNodeId == null) {
         return generations;
      }

      ArrayDeque<String> queue = new ArrayDeque<>();
      generations.put(anchorNodeId, 0);
      queue.add(anchorNodeId);

      while (!queue.isEmpty()) {
         String current = queue.removeFirst();
         int currentGeneration = generations.getOrDefault(current, 0);

         for (FamilyTreeEdgePayload edge : this.edges) {
            Integer nextGeneration = null;
            String nextNodeId = null;
            if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.PARENT_CHILD) {
               if (edge.getFromNodeId().equals(current)) {
                  nextNodeId = edge.getToNodeId();
                  nextGeneration = this.isPlayerNode(nextNodeId) ? currentGeneration - 1 : currentGeneration + 1;
               } else if (edge.getToNodeId().equals(current)) {
                  nextNodeId = edge.getFromNodeId();
                  nextGeneration = this.isPlayerNode(current) ? currentGeneration + 1 : currentGeneration - 1;
               }
            } else if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.SPOUSE || edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.FORMER_SPOUSE) {
               if (edge.getFromNodeId().equals(current)) {
                  nextNodeId = edge.getToNodeId();
                  nextGeneration = currentGeneration;
               } else if (edge.getToNodeId().equals(current)) {
                  nextNodeId = edge.getFromNodeId();
                  nextGeneration = currentGeneration;
               }
            }

            if (nextNodeId != null && !generations.containsKey(nextNodeId)) {
               generations.put(nextNodeId, nextGeneration);
               queue.add(nextNodeId);
            }
         }
      }

      for (FamilyTreeNodePayload node : this.nodes) {
         generations.putIfAbsent(node.getNodeId(), 0);
      }

      Integer rootGeneration = generations.get(this.findRootNodeId());
      if (rootGeneration != null && rootGeneration != 0) {
         Map<String, Integer> normalized = new HashMap<>();

         for (Entry<String, Integer> entry : generations.entrySet()) {
            normalized.put(entry.getKey(), entry.getValue() - rootGeneration);
         }

         return normalized;
      } else {
         return generations;
      }
   }

   private boolean isPlayerNode(String nodeId) {
      FamilyTreeNodePayload node = this.nodesById.get(nodeId);
      return node != null && node.getNodeType() == FamilyTreeNodePayload.NodeType.PLAYER;
   }

   private void centerOnRoot() {
      if (!this.centered) {
         String rootNodeId = this.findRootNodeId();
         FamilyTreeScreen.NodeLayout rootLayout = rootNodeId == null ? null : this.layoutById.get(rootNodeId);
         if (rootLayout == null) {
            this.panX = this.getViewportWidth() / 2.0 - 22.0;
            this.panY = this.getViewportHeight() / 2.0 - 22.0;
         } else {
            this.panX = this.getViewportWidth() / 2.0 - (rootLayout.x + 22.0);
            this.panY = this.getViewportHeight() / 2.0 - (rootLayout.y + 22.0);
         }

         this.clampPan();
         this.centered = true;
      }
   }

   private void clampPan() {
      int contentWidth = this.maxContentX - this.minContentX;
      int contentHeight = this.maxContentY - this.minContentY;
      int viewportWidth = this.getViewportWidth();
      int viewportHeight = this.getViewportHeight();
      if (contentWidth <= viewportWidth) {
         this.panX = (viewportWidth - contentWidth) / 2.0 - this.minContentX;
      } else {
         double minPanX = viewportWidth - this.maxContentX;
         double maxPanX = -this.minContentX;
         this.panX = Mth.clamp(this.panX, minPanX, maxPanX);
      }

      if (contentHeight <= viewportHeight) {
         this.panY = (viewportHeight - contentHeight) / 2.0 - this.minContentY;
      } else {
         double minPanY = viewportHeight - this.maxContentY;
         double maxPanY = -this.minContentY;
         this.panY = Mth.clamp(this.panY, minPanY, maxPanY);
      }
   }

   private String findAnchorNodeId() {
      String current = this.findRootNodeId();
      if (current == null) {
         return null;
      }

      Set<String> visited = new HashSet<>();

      while (visited.add(current)) {
         String selectedParent = null;

         for (FamilyTreeEdgePayload edge : this.edges) {
            if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.PARENT_CHILD
               && edge.getToNodeId().equals(current)
               && (selectedParent == null || this.compareNodeIds(edge.getFromNodeId(), selectedParent) < 0)) {
               selectedParent = edge.getFromNodeId();
            }
         }

         if (selectedParent == null) {
            return current;
         }

         current = selectedParent;
      }

      return current;
   }

   private String findRootNodeId() {
      for (FamilyTreeNodePayload node : this.nodes) {
         if (this.rootVillagerUuid != null && this.rootVillagerUuid.equals(node.getUuid())) {
            return node.getNodeId();
         }
      }

      return this.nodes.isEmpty() ? null : this.nodes.get(0).getNodeId();
   }

   private int getPortraitBorder(FamilyTreeNodePayload node) {
      if (node.isPlaceholder()) {
         return -11184811;
      } else if (node.getNodeType() == FamilyTreeNodePayload.NodeType.PLAYER) {
         return -9459457;
      } else if (this.rootVillagerUuid != null && this.rootVillagerUuid.equals(node.getUuid())) {
         return -12069049;
      } else {
         int descendantBorderColor = this.getDescendantSpouseBorderColor(node);
         if (descendantBorderColor != -1) {
            return descendantBorderColor;
         } else {
            return !node.isAlive() && node.getNodeType() == FamilyTreeNodePayload.NodeType.VILLAGER ? -6645094 : -1513240;
         }
      }
   }

   private void drawPortraitCornerMask(GuiGraphics graphics, int x, int y) {
      int maskColor = -15066598;
      graphics.fill(x, y, x + 3, y + 1, maskColor);
      graphics.fill(x, y + 1, x + 1, y + 3, maskColor);
      graphics.fill(x + 34 - 3, y, x + 34, y + 1, maskColor);
      graphics.fill(x + 34 - 1, y + 1, x + 34, y + 3, maskColor);
      graphics.fill(x, y + 34 - 1, x + 3, y + 34, maskColor);
      graphics.fill(x, y + 34 - 3, x + 1, y + 34 - 1, maskColor);
      graphics.fill(x + 34 - 3, y + 34 - 1, x + 34, y + 34, maskColor);
      graphics.fill(x + 34 - 1, y + 34 - 3, x + 34, y + 34 - 1, maskColor);
   }

   private FamilyTreeEdgePayload findSpouseEdge(String nodeId, Set<String> usedEdges) {
      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.SPOUSE && (edge.getFromNodeId().equals(nodeId) || edge.getToNodeId().equals(nodeId))) {
            return edge;
         }
      }

      return null;
   }

   private void markSpouseEdgesAsUsed(FamilyTreeScreen.FamilyBlock block, Set<String> usedEdges) {
      for (int i = 0; i < block.parentNodeIds().size(); i++) {
         for (int j = i + 1; j < block.parentNodeIds().size(); j++) {
            String spouseLeft = block.parentNodeIds().get(i);
            String spouseRight = block.parentNodeIds().get(j);

            for (FamilyTreeEdgePayload edge : this.edges) {
               if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.SPOUSE
                  && (
                     edge.getFromNodeId().equals(spouseLeft) && edge.getToNodeId().equals(spouseRight)
                        || edge.getFromNodeId().equals(spouseRight) && edge.getToNodeId().equals(spouseLeft)
                  )) {
                  usedEdges.add(this.edgeKey(edge));
               }
            }
         }
      }
   }

   private String edgeKey(FamilyTreeEdgePayload edge) {
      if (edge.getEdgeType() != FamilyTreeEdgePayload.EdgeType.SPOUSE && edge.getEdgeType() != FamilyTreeEdgePayload.EdgeType.FORMER_SPOUSE) {
         return edge.getEdgeType().name() + "|" + edge.getFromNodeId() + "|" + edge.getToNodeId();
      }

      String first = edge.getFromNodeId().compareTo(edge.getToNodeId()) <= 0 ? edge.getFromNodeId() : edge.getToNodeId();
      String second = first.equals(edge.getFromNodeId()) ? edge.getToNodeId() : edge.getFromNodeId();
      return edge.getEdgeType().name() + "|" + first + "|" + second;
   }

   private int getBaseFamilyGeneration(Iterable<FamilyTreeScreen.FamilyBlock> familyBlocks) {
      int baseFamilyGeneration = Integer.MAX_VALUE;

      for (FamilyTreeScreen.FamilyBlock block : familyBlocks) {
         for (String parentId : block.parentNodeIds()) {
            if (this.layoutById.containsKey(parentId)) {
               baseFamilyGeneration = Math.min(baseFamilyGeneration, this.lastComputedGenerations.getOrDefault(parentId, 0));
            }
         }
      }

      if (baseFamilyGeneration != Integer.MAX_VALUE) {
         return baseFamilyGeneration;
      }

      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.SPOUSE
            && this.layoutById.containsKey(edge.getFromNodeId())
            && this.layoutById.containsKey(edge.getToNodeId())) {
            baseFamilyGeneration = Math.min(baseFamilyGeneration, this.lastComputedGenerations.getOrDefault(edge.getFromNodeId(), 0));
            baseFamilyGeneration = Math.min(baseFamilyGeneration, this.lastComputedGenerations.getOrDefault(edge.getToNodeId(), 0));
         }
      }

      return baseFamilyGeneration == Integer.MAX_VALUE ? 0 : baseFamilyGeneration;
   }

   private int getFamilyBlockLineColor(FamilyTreeScreen.FamilyBlock block, int baseFamilyGeneration) {
      int maxDepth = 0;

      for (String parentId : block.parentNodeIds()) {
         maxDepth = Math.max(maxDepth, this.lastComputedGenerations.getOrDefault(parentId, baseFamilyGeneration) - baseFamilyGeneration);
      }

      return this.getGenerationLineColor(maxDepth);
   }

   private int getSpouseLineColor(FamilyTreeEdgePayload edge, int baseFamilyGeneration) {
      int fromGeneration = this.lastComputedGenerations.getOrDefault(edge.getFromNodeId(), baseFamilyGeneration);
      int toGeneration = this.lastComputedGenerations.getOrDefault(edge.getToNodeId(), baseFamilyGeneration);
      return this.getGenerationLineColor(Math.min(fromGeneration, toGeneration) - baseFamilyGeneration);
   }

   private int getGenerationLineColor(int depth) {
      if (depth <= 0) {
         return -1644826;
      } else if (depth == 1) {
         return -7350363;
      } else {
         return depth == 2 ? -13653937 : -14714310;
      }
   }

   private int getDescendantSpouseBorderColor(FamilyTreeNodePayload node) {
      if (node != null && node.getNodeType() == FamilyTreeNodePayload.NodeType.VILLAGER) {
         int baseFamilyGeneration = this.getBaseFamilyGeneration(this.getVisibleFamilyBlocks());
         int nodeGeneration = this.lastComputedGenerations.getOrDefault(node.getNodeId(), baseFamilyGeneration);
         if (nodeGeneration <= baseFamilyGeneration) {
            return -1;
         }

         List<String> spouseIds = this.getSpouseIds(node.getNodeId());
         if (spouseIds.isEmpty()) {
            return -1;
         }

         for (String spouseId : spouseIds) {
            int spouseGeneration = this.lastComputedGenerations.getOrDefault(spouseId, baseFamilyGeneration);
            if (spouseGeneration == nodeGeneration && !this.getVisibleChildrenForPair(node.getNodeId(), spouseId).isEmpty()) {
               boolean nodeHasVisibleParents = this.hasVisibleParents(node.getNodeId());
               boolean spouseHasVisibleParents = this.hasVisibleParents(spouseId);
               boolean spouseIsAncestorOfNode = this.isParentOrAncestor(spouseId, node.getNodeId());
               boolean nodeIsAncestorOfSpouse = this.isParentOrAncestor(node.getNodeId(), spouseId);
               if (!nodeHasVisibleParents && spouseHasVisibleParents && !spouseIsAncestorOfNode) {
                  return this.getGenerationLineColor(nodeGeneration - baseFamilyGeneration);
               }

               if (nodeHasVisibleParents && !spouseHasVisibleParents && !nodeIsAncestorOfSpouse) {
               }
            }
         }

         return -1;
      } else {
         return -1;
      }
   }

   private boolean hasVisibleParents(String nodeId) {
      return !this.getParentIds(nodeId).isEmpty();
   }

   private boolean isParentOrAncestor(String possibleAncestorId, String nodeId) {
      if (possibleAncestorId != null && nodeId != null && !possibleAncestorId.equals(nodeId)) {
         ArrayDeque<String> queue = new ArrayDeque<>();
         Set<String> visited = new HashSet<>();
         queue.add(nodeId);
         visited.add(nodeId);

         while (!queue.isEmpty()) {
            String currentId = queue.removeFirst();

            for (String parentId : this.getParentIds(currentId)) {
               if (possibleAncestorId.equals(parentId)) {
                  return true;
               }

               if (visited.add(parentId)) {
                  queue.addLast(parentId);
               }
            }
         }

         return false;
      } else {
         return false;
      }
   }

   private List<FamilyTreeScreen.FamilyBlock> getVisibleFamilyBlocks() {
      Map<String, FamilyTreeScreen.FamilyBlock> familyBlocks = new LinkedHashMap<>();
      Map<String, List<FamilyTreeEdgePayload>> parentEdgesByChild = new HashMap<>();

      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.PARENT_CHILD) {
            parentEdgesByChild.computeIfAbsent(edge.getToNodeId(), ignored -> new ArrayList<>()).add(edge);
         }
      }

      for (Entry<String, List<FamilyTreeEdgePayload>> entry : parentEdgesByChild.entrySet()) {
         FamilyTreeNodePayload childNode = this.nodesById.get(entry.getKey());
         if (childNode == null || childNode.getNodeType() != FamilyTreeNodePayload.NodeType.PLAYER) {
            List<String> parentIds = new ArrayList<>();

            for (FamilyTreeEdgePayload edge : entry.getValue()) {
               if (this.layoutById.containsKey(edge.getFromNodeId())) {
                  parentIds.add(edge.getFromNodeId());
               }
            }

            if (parentIds.size() < 2 && !parentIds.isEmpty()) {
               FamilyTreeEdgePayload spouseEdge = this.findSpouseEdge(parentIds.get(0), Set.of());
               if (spouseEdge != null) {
                  String spouseId = spouseEdge.getFromNodeId().equals(parentIds.get(0)) ? spouseEdge.getToNodeId() : spouseEdge.getFromNodeId();
                  if (this.layoutById.containsKey(spouseId)) {
                     parentIds.add(spouseId);
                  }
               }
            }

            if (parentIds.size() >= 2) {
               parentIds.sort(this::compareNodeIds);
               FamilyTreeScreen.FamilyBlock block = familyBlocks.computeIfAbsent(
                  String.join("|", parentIds), ignored -> new FamilyTreeScreen.FamilyBlock(parentIds)
               );
               if (!block.childNodeIds().contains(entry.getKey())) {
                  block.childNodeIds().add(entry.getKey());
               }
            }
         }
      }

      return new ArrayList<>(familyBlocks.values());
   }

   private List<String> getVisibleChildrenForPair(String firstParentId, String secondParentId) {
      List<String> childIds = new ArrayList<>();
      int expectedGeneration = Math.max(
            this.lastComputedGenerations.getOrDefault(firstParentId, 0), this.lastComputedGenerations.getOrDefault(secondParentId, 0)
         )
         + 1;
      Set<String> candidateChildIds = new HashSet<>(this.getChildIds(firstParentId));
      candidateChildIds.addAll(this.getChildIds(secondParentId));

      for (String childId : candidateChildIds) {
         List<String> parentIds = this.getParentIds(childId);
         if (this.lastComputedGenerations.getOrDefault(childId, Integer.MIN_VALUE) == expectedGeneration
            && parentIds.contains(firstParentId)
            && parentIds.contains(secondParentId)) {
            childIds.add(childId);
         }
      }

      childIds.sort(this::compareNodeIds);
      return childIds;
   }

   private List<String> findSharedChildren(String firstParentId, String secondParentId) {
      List<String> sharedChildren = new ArrayList<>();

      for (FamilyTreeNodePayload node : this.nodes) {
         if (node.getNodeType() == FamilyTreeNodePayload.NodeType.VILLAGER
            && this.isChildLinkedToParent(node.getNodeId(), firstParentId)
            && this.isChildLinkedToParent(node.getNodeId(), secondParentId)) {
            sharedChildren.add(node.getNodeId());
         }
      }

      sharedChildren.sort(this::compareNodeIds);
      return sharedChildren;
   }

   private boolean isFormerSpousePair(String firstNodeId, String secondNodeId) {
      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.FORMER_SPOUSE
            && (
               edge.getFromNodeId().equals(firstNodeId) && edge.getToNodeId().equals(secondNodeId)
                  || edge.getFromNodeId().equals(secondNodeId) && edge.getToNodeId().equals(firstNodeId)
            )) {
            return true;
         }
      }

      return false;
   }

   private boolean isChildLinkedToParent(String childNodeId, String parentNodeId) {
      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.PARENT_CHILD
            && (
               edge.getFromNodeId().equals(parentNodeId) && edge.getToNodeId().equals(childNodeId)
                  || edge.getFromNodeId().equals(childNodeId) && edge.getToNodeId().equals(parentNodeId)
            )) {
            return true;
         }
      }

      return false;
   }

   private boolean isParentChildLinkForNodes(FamilyTreeEdgePayload edge, String childNodeId, String firstParentId, String secondParentId) {
      return edge.getFromNodeId().equals(firstParentId) && edge.getToNodeId().equals(childNodeId)
         || edge.getFromNodeId().equals(secondParentId) && edge.getToNodeId().equals(childNodeId)
         || edge.getFromNodeId().equals(childNodeId) && edge.getToNodeId().equals(firstParentId)
         || edge.getFromNodeId().equals(childNodeId) && edge.getToNodeId().equals(secondParentId);
   }

   private static void drawFamilyLine(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
      drawFamilyLine(graphics, x1, y1, x2, y2, -1644826);
   }

   private static void drawFamilyLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
      drawThickLine(graphics, x1, y1, x2, y2, color, 3);
   }

   private static void drawThickLine(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, int thickness) {
      int half = thickness / 2;
      if (x1 == x2) {
         graphics.fill(x1 - half, Math.min(y1, y2), x1 - half + thickness, Math.max(y1, y2) + 1, color);
      } else if (y1 == y2) {
         graphics.fill(Math.min(x1, x2), y1 - half, Math.max(x1, x2) + 1, y1 - half + thickness, color);
      }
   }

   private String resolveLifeStage(String ageStage) {
      return switch (ageStage == null ? "" : ageStage) {
         case "TODDLER" -> Component.translatable("slimpatch.life_stage.toddler").getString();
         case "CHILD" -> Component.translatable("slimpatch.life_stage.child").getString();
         case "TEEN" -> Component.translatable("slimpatch.life_stage.teen").getString();
         case "ADULT" -> Component.translatable("slimpatch.life_stage.adult").getString();
         default -> ageStage != null && !ageStage.isEmpty() ? ageStage : Component.translatable("slimpatch.screen.family_tree.unknown").getString();
      };
   }

   private List<String> getParentIds(String nodeId) {
      return this.getParentIds(nodeId, this.lastComputedGenerations);
   }

   private List<String> getParentIds(String nodeId, Map<String, Integer> generations) {
      Set<String> parentIds = new HashSet<>();
      int nodeGeneration = generations.getOrDefault(nodeId, 0);

      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.PARENT_CHILD) {
            if (edge.getToNodeId().equals(nodeId)) {
               if (generations.getOrDefault(edge.getFromNodeId(), nodeGeneration) < nodeGeneration) {
                  parentIds.add(edge.getFromNodeId());
               }
            } else if (edge.getFromNodeId().equals(nodeId) && generations.getOrDefault(edge.getToNodeId(), nodeGeneration) < nodeGeneration) {
               parentIds.add(edge.getToNodeId());
            }
         }
      }

      List<String> sortedParentIds = new ArrayList<>(parentIds);
      sortedParentIds.sort(this::compareNodeIds);
      return sortedParentIds.size() > 2 ? new ArrayList<>(sortedParentIds.subList(0, 2)) : sortedParentIds;
   }

   private List<String> getChildIds(String nodeId) {
      Set<String> childIds = new HashSet<>();
      int nodeGeneration = this.lastComputedGenerations.getOrDefault(nodeId, 0);

      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.PARENT_CHILD) {
            if (edge.getFromNodeId().equals(nodeId)) {
               if (this.lastComputedGenerations.getOrDefault(edge.getToNodeId(), nodeGeneration) > nodeGeneration) {
                  childIds.add(edge.getToNodeId());
               }
            } else if (edge.getToNodeId().equals(nodeId) && this.lastComputedGenerations.getOrDefault(edge.getFromNodeId(), nodeGeneration) > nodeGeneration) {
               childIds.add(edge.getFromNodeId());
            }
         }
      }

      List<String> sortedChildIds = new ArrayList<>(childIds);
      sortedChildIds.sort(this::compareNodeIds);
      return sortedChildIds;
   }

   private List<String> getSiblingIds(String nodeId) {
      Set<String> siblingIds = new HashSet<>();

      for (String parentId : this.getParentIds(nodeId)) {
         for (String childId : this.getChildIds(parentId)) {
            if (!childId.equals(nodeId)) {
               siblingIds.add(childId);
            }
         }
      }

      List<String> sortedSiblingIds = new ArrayList<>(siblingIds);
      sortedSiblingIds.sort(this::compareNodeIds);
      return sortedSiblingIds;
   }

   private List<String> getSpouseIds(String nodeId) {
      Set<String> spouseIds = new HashSet<>();

      for (FamilyTreeEdgePayload edge : this.edges) {
         if (edge.getEdgeType() == FamilyTreeEdgePayload.EdgeType.SPOUSE) {
            if (edge.getFromNodeId().equals(nodeId)) {
               spouseIds.add(edge.getToNodeId());
            } else if (edge.getToNodeId().equals(nodeId)) {
               spouseIds.add(edge.getFromNodeId());
            }
         }
      }

      List<String> sortedSpouseIds = new ArrayList<>(spouseIds);
      sortedSpouseIds.sort(this::compareNodeIds);
      return sortedSpouseIds;
   }

   private void addRelatedNode(Map<String, List<String>> relatedNodeIdsByNode, String nodeId, String relatedNodeId) {
      List<String> relatedNodeIds = relatedNodeIdsByNode.computeIfAbsent(nodeId, ignored -> new ArrayList<>());
      if (!relatedNodeIds.contains(relatedNodeId)) {
         relatedNodeIds.add(relatedNodeId);
         relatedNodeIds.sort(this::compareNodeIds);
      }
   }

   private String getRelationToVisiblePlayer(String nodeId) {
      String playerNodeId = this.findVisiblePlayerNodeId();
      if (playerNodeId != null && !playerNodeId.equals(nodeId)) {
         ArrayDeque<String> queue = new ArrayDeque<>();
         ArrayDeque<Integer> distances = new ArrayDeque<>();
         Set<String> visited = new HashSet<>();
         queue.add(playerNodeId);
         distances.add(0);
         visited.add(playerNodeId);

         while (!queue.isEmpty()) {
            String currentId = queue.removeFirst();
            int distance = distances.removeFirst();

            for (String childId : this.getChildIds(currentId)) {
               if (visited.add(childId)) {
                  int nextDistance = distance + 1;
                  if (childId.equals(nodeId)) {
                     return switch (nextDistance) {
                        case 1 -> Component.translatable("slimpatch.screen.family_tree.relation.child").getString();
                        case 2 -> Component.translatable("slimpatch.screen.family_tree.relation.grandchild").getString();
                        case 3 -> Component.translatable("slimpatch.screen.family_tree.relation.great_grandchild").getString();
                        default -> Component.translatable("slimpatch.screen.family_tree.relation.descendant").getString();
                     };
                  }

                  queue.addLast(childId);
                  distances.addLast(nextDistance);
               }
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private String findVisiblePlayerNodeId() {
      List<String> playerIds = new ArrayList<>();

      for (FamilyTreeNodePayload node : this.nodes) {
         if (node.getNodeType() == FamilyTreeNodePayload.NodeType.PLAYER) {
            playerIds.add(node.getNodeId());
         }
      }

      if (playerIds.isEmpty()) {
         return null;
      }

      playerIds.sort(this::compareNodeIds);
      return playerIds.get(0);
   }

   private String formatLimitedNameList(List<String> nodeIds, int limit, boolean showMore) {
      if (nodeIds.isEmpty()) {
         return "";
      } else {
         List<String> names = this.formatNodeNameList(nodeIds);
         if (names.isEmpty()) {
            return "";
         } else if (names.size() > limit && showMore) {
            int remaining = names.size() - limit;
            return String.join(", ", names.subList(0, limit))
               + " "
               + Component.translatable("slimpatch.screen.family_tree.more", new Object[]{remaining}).getString();
         } else {
            return String.join(", ", names.subList(0, Math.min(limit, names.size())));
         }
      }
   }

   private List<String> formatNodeNameList(List<String> nodeIds) {
      List<String> sortedNodeIds = new ArrayList<>(new HashSet<>(nodeIds));
      sortedNodeIds.sort(this::compareNodeIds);
      List<String> names = new ArrayList<>();
      Set<String> seenNames = new HashSet<>();

      for (String nodeId : sortedNodeIds) {
         String name = this.getNodeDisplayName(nodeId);
         if (!name.isEmpty() && seenNames.add(name)) {
            names.add(name);
         }
      }

      return names;
   }

   private String getNodeDisplayName(String nodeId) {
      FamilyTreeNodePayload node = this.nodesById.get(nodeId);
      return node != null && !node.getDisplayName().isEmpty()
         ? node.getDisplayName()
         : Component.translatable("slimpatch.screen.family_tree.placeholder_name").getString();
   }

   private boolean isMouseInsideViewport(double mouseX, double mouseY) {
      int left = (this.width - this.getWindowWidth()) / 2 + 9;
      int top = (this.height - this.getWindowHeight()) / 2 + 18;
      return mouseX >= left && mouseX <= left + this.getViewportWidth() && mouseY >= top && mouseY <= top + this.getViewportHeight();
   }

   private void drawBoldCenteredString(GuiGraphics graphics, String text, int centerX, int y, int color) {
      graphics.drawCenteredString(this.font, text, centerX, y, color);
      graphics.drawCenteredString(this.font, text, centerX + 1, y, color);
   }

   private int getWindowWidth() {
      return this.expandedView ? Math.max(280, this.width - 32) : Math.min(360, Math.max(280, this.width - 24));
   }

   private int getWindowHeight() {
      return this.expandedView ? Math.max(180, this.height - 32) : Math.min(220, Math.max(180, this.height - 24));
   }

   private int getViewportWidth() {
      return this.getWindowWidth() - 9 - 9;
   }

   private int getViewportHeight() {
      return this.getWindowHeight() - 18 - 18;
   }

   private int compareNodeIds(String first, String second) {
      FamilyTreeNodePayload firstNode = this.nodesById.get(first);
      FamilyTreeNodePayload secondNode = this.nodesById.get(second);
      if (firstNode != null && secondNode != null) {
         int byType = Integer.compare(firstNode.getNodeType().ordinal(), secondNode.getNodeType().ordinal());
         if (byType != 0) {
            return byType;
         }

         int byName = firstNode.getDisplayName().compareTo(secondNode.getDisplayName());
         if (byName != 0) {
            return byName;
         }
      }

      return first.compareTo(second);
   }

   private void releaseViewHold() {
      if (!this.closePacketSent) {
         Minecraft minecraft = Minecraft.getInstance();
         if (minecraft.player != null) {
            ModNetworking.sendToServer(new CloseFamilyTreePacket());
         }

         this.closePacketSent = true;
      }
   }

   private record ChildPlacement(FamilyTreeScreen.FamilyUnit unit, int centerX) {
   }

   private record FamilyBlock(List<String> parentNodeIds, List<String> childNodeIds) {
      private FamilyBlock(List<String> parentNodeIds) {
         this(parentNodeIds, new ArrayList<>());
      }
   }

   private record FamilyUnit(
      String anchorNodeId,
      String spouseNodeId,
      List<String> childNodeIds,
      List<FamilyTreeScreen.FamilyUnit> childUnits,
      FamilyTreeScreen.UnitType unitType,
      FamilyTreeScreen.SpouseSide spouseSide
   ) {
   }

   private record LayoutContext(
      Map<String, Integer> generations,
      Map<String, List<String>> spouseIdsByNode,
      Map<String, List<String>> partnerIdsByNode,
      Map<String, List<String>> parentIdsByChild,
      Map<String, List<String>> childIdsByFamilyKey,
      Map<String, List<String>> siblingIdsByNode,
      Map<Integer, List<String>> nodeIdsByGeneration,
      Map<Integer, Set<String>> prelaidOutNodeIdsByGeneration
   ) {
   }

   private record NodeLayout(int x, int y) {
   }

   private enum SpouseSide {
      LEFT,
      RIGHT;
   }

   private enum UnitType {
      SINGLE,
      COUPLE,
      DESCENDANT_FAMILY;
   }
}
