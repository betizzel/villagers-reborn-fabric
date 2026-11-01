package com.javic.slimpatch.dialogue;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonStreamParser;
import com.google.gson.reflect.TypeToken;
import com.javic.slimpatch.entity.FamilyVillager;
import com.javic.slimpatch.entity.FemaleVillagerEntity;
import com.javic.slimpatch.entity.MaleVillagerEntity;
import com.javic.slimpatch.entity.VillagerAgeStage;
import com.javic.slimpatch.entity.VillagerPersonality;
import com.javic.slimpatch.entity.VillagerRelationshipData;
import com.javic.slimpatch.entity.VillagerRelationshipStage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

public class DialogueManager {
   private static final Map<UUID, UUID> ACTIVE_DIALOGUES = new HashMap<>();
   private static final Map<String, Map<String, Object>> DIALOGUES = new HashMap<>();
   private static final Map<String, Map<String, Object>> DATING_DIALOGUES = new HashMap<>();
   private static final Map<String, Map<String, Object>> MARRIED_DIALOGUES = new HashMap<>();
   private static final Map<String, Map<String, Object>> TODDLER_DIALOGUES = new HashMap<>();
   private static final Map<String, Map<String, Object>> CHILD_DIALOGUES = new HashMap<>();
   private static final Map<String, Map<String, Object>> TEEN_DIALOGUES = new HashMap<>();
   private static final Map<String, DialogueManager.DialogueSet> LOCALIZED_DIALOGUES = new HashMap<>();
   private static final DialogueManager.DialogueSet EMPTY_DIALOGUE_SET = new DialogueManager.DialogueSet(
      Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap(), Collections.emptyMap()
   );
   private static final Random RAND = new Random();
   private static final Map<VillagerPersonality, Map<String, Integer>> PERSONALITY_MODIFIERS = Map.ofEntries(
      Map.entry(VillagerPersonality.FRIENDLY, Map.of("Friendly", 20, "Mean", -20, "Joke", 10, "Flirt", 0)),
      Map.entry(VillagerPersonality.MEAN, Map.of("Friendly", -20, "Mean", 20, "Joke", -10, "Flirt", -10)),
      Map.entry(VillagerPersonality.SHY, Map.of("Friendly", 15, "Mean", -15, "Joke", 10, "Flirt", -10)),
      Map.entry(VillagerPersonality.BRAVE, Map.of("Friendly", 10, "Mean", 10, "Joke", 0, "Flirt", 10)),
      Map.entry(VillagerPersonality.GRUMPY, Map.of("Friendly", -20, "Mean", 15, "Joke", -15, "Flirt", -10)),
      Map.entry(VillagerPersonality.GREEDY, Map.of("Friendly", 0, "Mean", -10, "Joke", 0, "Flirt", 10)),
      Map.entry(VillagerPersonality.ROMANTIC, Map.of("Friendly", 10, "Mean", -20, "Joke", 10, "Flirt", 25)),
      Map.entry(VillagerPersonality.WISE, Map.of("Friendly", 10, "Mean", -10, "Joke", 10, "Flirt", -5))
   );

   public static void startDialogue(Villager villager, Player player) {
      if (villager != null && player != null) {
         ACTIVE_DIALOGUES.put(villager.getUUID(), player.getUUID());
      }
   }

   public static void endDialogue(Villager villager) {
      if (villager != null) {
         ACTIVE_DIALOGUES.remove(villager.getUUID());
      }
   }

   public static void endDialogue(Villager villager, Player player) {
      if (villager != null && player != null) {
         UUID playerId = ACTIVE_DIALOGUES.get(villager.getUUID());
         if (player.getUUID().equals(playerId)) {
            ACTIVE_DIALOGUES.remove(villager.getUUID());
         }
      }
   }

   public static void endDialoguesForPlayer(UUID playerId) {
      if (playerId != null) {
         ACTIVE_DIALOGUES.entrySet().removeIf(entry -> playerId.equals(entry.getValue()));
      }
   }

   public static boolean isInDialogue(Villager villager) {
      return villager != null && ACTIVE_DIALOGUES.containsKey(villager.getUUID());
   }

   public static UUID getDialoguePlayer(Villager villager) {
      return villager != null ? ACTIVE_DIALOGUES.get(villager.getUUID()) : null;
   }

   public static Map<UUID, UUID> getActiveDialogues() {
      return new HashMap<>(ACTIVE_DIALOGUES);
   }

   private static void loadDialogues() {
      DIALOGUES.clear();
      DATING_DIALOGUES.clear();
      MARRIED_DIALOGUES.clear();
      TODDLER_DIALOGUES.clear();
      CHILD_DIALOGUES.clear();
      TEEN_DIALOGUES.clear();
      LOCALIZED_DIALOGUES.clear();
      DIALOGUES.putAll(loadDialogueFile("assets/slimpatch/dialogues/dialogues.json"));
      DATING_DIALOGUES.putAll(loadDialogueFile("assets/slimpatch/dialogues/dating_dialogues.json"));
      MARRIED_DIALOGUES.putAll(loadDialogueFile("assets/slimpatch/dialogues/married_dialogues.json"));
      TODDLER_DIALOGUES.putAll(loadDialogueFile("assets/slimpatch/dialogues/toddler_dialogues.json"));
      CHILD_DIALOGUES.putAll(loadDialogueFile("assets/slimpatch/dialogues/child_dialogues.json"));
      TEEN_DIALOGUES.putAll(loadDialogueFile("assets/slimpatch/dialogues/teen_dialogues.json"));
   }

   private static Map<String, Map<String, Object>> loadDialogueFile(String path) {
      try {
         InputStream resource = DialogueManager.class.getClassLoader().getResourceAsStream(path);
         if (resource == null) {
            return Collections.emptyMap();
         }

         InputStreamReader reader = new InputStreamReader(resource, StandardCharsets.UTF_8);
         Type type = (new TypeToken<Map<String, Map<String, Object>>>() {}).getType();
         Map<String, Map<String, Object>> data = new LinkedHashMap<>();
         JsonStreamParser parser = new JsonStreamParser(reader);
         Gson gson = new Gson();

         while (parser.hasNext()) {
            JsonElement element = parser.next();
            if (element != null && element.isJsonObject()) {
               Map<String, Map<String, Object>> parsed = (Map<String, Map<String, Object>>)gson.fromJson((JsonObject)element, type);
               mergeDialogueMaps(data, parsed);
            }
         }

         reader.close();
         if (!data.isEmpty()) {
            return data;
         }
      } catch (Exception var9) {
      }

      return Collections.emptyMap();
   }

   private static void mergeDialogueMaps(Map<String, Map<String, Object>> target, Map<String, Map<String, Object>> source) {
      if (target != null && source != null && !source.isEmpty()) {
         for (Entry<String, Map<String, Object>> entry : source.entrySet()) {
            Map<String, Object> targetSection = target.computeIfAbsent(entry.getKey(), ignored -> new LinkedHashMap<>());
            Map<String, Object> sourceSection = entry.getValue();
            if (sourceSection != null && !sourceSection.isEmpty()) {
               for (Entry<String, Object> personalityEntry : sourceSection.entrySet()) {
                  Object sourceValue = personalityEntry.getValue();
                  if (targetSection.get(personalityEntry.getKey()) instanceof Map<?, ?> targetMap && sourceValue instanceof Map<?, ?> sourceMap) {
                     ((Map<Object, Object>)targetMap).putAll(sourceMap);
                  } else {
                     targetSection.put(personalityEntry.getKey(), sourceValue);
                  }
               }
            }
         }
      }
   }

   public static boolean calculateSuccess(VillagerPersonality personality, String option) {
      if (personality == null || option == null) {
         return true;
      }

      if (option.equalsIgnoreCase("Mean") && personality != VillagerPersonality.GRUMPY && personality != VillagerPersonality.MEAN) {
         return false;
      }

      int baseChance = 70;
      int modifier = PERSONALITY_MODIFIERS.getOrDefault(personality, Collections.emptyMap()).getOrDefault(option, 0);
      int finalChance = Math.max(15, Math.min(95, baseChance + modifier));
      int roll = RAND.nextInt(100) + 1;
      return roll <= finalChance;
   }

   public static String getRandomLine(String category, Villager villager, boolean success) {
      if (villager != null && category != null) {
         VillagerPersonality personality = getPersonality(villager);
         if (personality == null) {
            return "...";
         }

         Map<String, Map<String, Object>> stageDialogues = getStageDialogues(villager);
         String personalityKey = personality.name().toLowerCase(Locale.ROOT);
         String stageLine = findRandomLineFromMap(stageDialogues, category, personalityKey, null, success);
         if (stageLine != null) {
            return stageLine;
         }

         String baseLine = findRandomLineFromMap(DIALOGUES, category, personalityKey, null, success);
         return baseLine == null ? "..." : baseLine;
      } else {
         return "...";
      }
   }

   public static String getRandomLine(String category, Villager villager) {
      return getRandomLine(category, villager, true);
   }

   public static String getRandomLine(String category, Villager villager, Player player, boolean success) {
      if (villager != null && category != null) {
         VillagerPersonality personality = getPersonality(villager);
         if (personality == null) {
            return "...";
         }

         DialogueManager.DialogueSet localizedDialogueSet = getLocalizedDialogueSet(player);
         Map<String, Map<String, Object>> stageDialogues = getStageDialogues(villager, player, localizedDialogueSet);
         String personalityKey = personality.name().toLowerCase(Locale.ROOT);
         String playerChildPersonalityKey = getPlayerChildPersonalityKey(villager, player, category, personalityKey);
         String stageLine = findRandomLineFromMap(stageDialogues, category, playerChildPersonalityKey, personalityKey, success);
         if (stageLine != null) {
            return stageLine;
         }

         String localizedLine = findRandomLineFromMap(localizedDialogueSet.dialogues(), category, personalityKey, null, success);
         if (localizedLine != null) {
            return localizedLine;
         }

         String baseLine = findRandomLineFromMap(DIALOGUES, category, personalityKey, null, success);
         return baseLine == null ? "..." : baseLine;
      } else {
         return "...";
      }
   }

   private static DialogueManager.DialogueSet getLocalizedDialogueSet(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         String language = resolveDialogueLanguage(com.javic.slimpatch.fabric.PlayerLanguage.get(serverPlayer));
         if (!language.isEmpty() && !"en_us".equals(language)) {
            DialogueManager.DialogueSet cached = LOCALIZED_DIALOGUES.get(language);
            if (cached != null) {
               return cached;
            }

            DialogueManager.DialogueSet loaded = new DialogueManager.DialogueSet(
               loadDialogueFile("assets/slimpatch/dialogues/" + language + "/dialogues.json"),
               loadDialogueFile("assets/slimpatch/dialogues/" + language + "/dating_dialogues.json"),
               loadDialogueFile("assets/slimpatch/dialogues/" + language + "/married_dialogues.json"),
               loadDialogueFile("assets/slimpatch/dialogues/" + language + "/toddler_dialogues.json"),
               loadDialogueFile("assets/slimpatch/dialogues/" + language + "/child_dialogues.json"),
               loadDialogueFile("assets/slimpatch/dialogues/" + language + "/teen_dialogues.json")
            );
            LOCALIZED_DIALOGUES.put(language, loaded);
            return loaded;
         } else {
            return EMPTY_DIALOGUE_SET;
         }
      } else {
         return EMPTY_DIALOGUE_SET;
      }
   }

   private static String normalizeLanguage(String language) {
      return language != null && !language.isBlank() ? language.toLowerCase(Locale.ROOT).replace('-', '_') : "";
   }

   private static String resolveDialogueLanguage(String language) {
      String normalized = normalizeLanguage(language);

      return switch (normalized) {
         case "es_es", "es_ar", "es_cl", "es_ec", "es_mx", "es_uy", "es_ve" -> "es_es";
         default -> normalized;
      };
   }

   public static float getRelationshipChange(VillagerPersonality personality, String option, boolean success) {
      if (option == null) {
         return 0.0F;
      } else if (option.equalsIgnoreCase("Mean") && personality != VillagerPersonality.GRUMPY && personality != VillagerPersonality.MEAN) {
         return -0.5F;
      } else if (success) {
         return option.equalsIgnoreCase("Flirt") ? 0.7F : 0.5F;
      } else {
         return option.equalsIgnoreCase("Flirt") ? -0.4F : -0.3F;
      }
   }

   public static String getQuestIntro(String questId) {
      if (DIALOGUES != null && !DIALOGUES.isEmpty()) {
         String matchedKey = null;

         for (String key : DIALOGUES.keySet()) {
            if (key.equalsIgnoreCase("QuestIntro")) {
               matchedKey = key;
               break;
            }
         }

         if (matchedKey == null) {
            return "...";
         }

         Map<String, Object> questIntro = DIALOGUES.get(matchedKey);
         if (questIntro == null) {
            return "...";
         }

         Object questEntry = null;

         for (String key : questIntro.keySet()) {
            if (key.equalsIgnoreCase(questId)) {
               questEntry = questIntro.get(key);
               break;
            }
         }

         if (questEntry == null) {
            return "...";
         } else {
            List<String> lines = extractLinesRecursive(questEntry);
            if (lines != null && !lines.isEmpty()) {
               int index = RAND.nextInt(lines.size());
               Object line = lines.get(index);
               return line instanceof String ? (String)line : "...";
            } else {
               return "...";
            }
         }
      } else {
         return "...";
      }
   }

   public static String getGenericQuestLine(String category) {
      if (DIALOGUES != null && !DIALOGUES.isEmpty()) {
         String matchedKey = null;

         for (String key : DIALOGUES.keySet()) {
            if (key.equalsIgnoreCase(category)) {
               matchedKey = key;
               break;
            }
         }

         if (matchedKey == null) {
            return "...";
         } else {
            Map<String, Object> section = DIALOGUES.get(matchedKey);
            if (section == null) {
               return "...";
            } else {
               List<String> lines = extractLinesRecursive(section);
               if (lines != null && !lines.isEmpty()) {
                  int index = RAND.nextInt(lines.size());
                  Object line = lines.get(index);
                  return line instanceof String ? (String)line : "...";
               } else {
                  return "...";
               }
            }
         }
      } else {
         return "...";
      }
   }

   private static List<String> extractLinesRecursive(Object node) {
      if (node instanceof Map<?, ?> map) {
         if (map.containsKey("lines") && map.get("lines") instanceof List<?> list) {
            List<String> result = new ArrayList<>();

            for (Object obj : list) {
               if (obj instanceof String) {
                  result.add((String)obj);
               }
            }

            return result;
         }

         for (Object value : map.values()) {
            List<String> found = extractLinesRecursive(value);
            if (found != null && !found.isEmpty()) {
               return found;
            }
         }
      }

      return null;
   }

   private static String getRandomLineFromMap(
      Map<String, Map<String, Object>> dialogues, String category, String personalityKey, String fallbackPersonalityKey, boolean success
   ) {
      String line = findRandomLineFromMap(dialogues, category, personalityKey, fallbackPersonalityKey, success);
      return line != null ? line : "...";
   }

   private static String findRandomLineFromMap(
      Map<String, Map<String, Object>> dialogues, String category, String personalityKey, String fallbackPersonalityKey, boolean success
   ) {
      if (dialogues == null || dialogues.isEmpty()) {
         return null;
      }
      Map<String, Object> byPersonality = getIgnoreCase(dialogues, category);
      if (byPersonality == null) {
         return null;
      }
      Object block = getDialogueBlock(byPersonality, personalityKey, fallbackPersonalityKey);
      if (block instanceof List<?> lines) {
         return lines.isEmpty() ? null : (String)lines.get(RAND.nextInt(lines.size()));
      }
      if (!(block instanceof Map<?, ?> map)) {
         return null;
      }
      Object value;
      if (!success && map.containsKey("fail")) {
         value = map.get("fail");
      } else {
         value = map.get("lines");
         if (!(value instanceof List<?>) && !(value instanceof String)) {
            value = map.get("default");
         }
      }
      if (value instanceof String single) {
         return single;
      }
      if (value instanceof List<?> lines && !lines.isEmpty()) {
         return (String)lines.get(RAND.nextInt(lines.size()));
      }
      return null;
   }

   private static Map<String, Map<String, Object>> getStageDialogues(Villager villager) {
      return getYoungStageDialogues(villager);
   }

   private static Map<String, Map<String, Object>> getStageDialogues(Villager villager, Player player) {
      return getStageDialogues(villager, player, EMPTY_DIALOGUE_SET);
   }

   private static Map<String, Map<String, Object>> getStageDialogues(Villager villager, Player player, DialogueManager.DialogueSet dialogueSet) {
      Map<String, Map<String, Object>> youngStageDialogues = getYoungStageDialogues(villager, dialogueSet);
      if (!youngStageDialogues.isEmpty()) {
         return youngStageDialogues;
      }

      if (villager instanceof FamilyVillager familyVillager) {
         if (familyVillager.getRelationshipStage() == VillagerRelationshipStage.MARRIED) {
            if (player == null) {
               return dialogueSet.marriedDialogues().isEmpty() ? MARRIED_DIALOGUES : dialogueSet.marriedDialogues();
            }

            if (!VillagerRelationshipData.usesPerPlayerRelationships(villager)) {
               return dialogueSet.marriedDialogues().isEmpty() ? MARRIED_DIALOGUES : dialogueSet.marriedDialogues();
            }

            if (player.getUUID().equals(familyVillager.getSpousePlayerUuid())) {
               return dialogueSet.marriedDialogues().isEmpty() ? MARRIED_DIALOGUES : dialogueSet.marriedDialogues();
            }

            return Collections.emptyMap();
         }

         if (player != null) {
            if (VillagerRelationshipData.usesPerPlayerRelationships(villager)) {
               float goldenRelationship = villager.level().isClientSide()
                  ? VillagerRelationshipData.getDisplayedGoldenRelationship(villager, 0.0F)
                  : VillagerRelationshipData.getGoldenRelationshipForPlayer(villager, player.getUUID(), 0.0F);
               if (goldenRelationship > 0.0F) {
                  return dialogueSet.datingDialogues().isEmpty() ? DATING_DIALOGUES : dialogueSet.datingDialogues();
               }

               return Collections.emptyMap();
            }

            float goldenRelationship = villager.level().isClientSide()
               ? VillagerRelationshipData.getDisplayedGoldenRelationship(villager, familyVillager.getGoldenRelationship())
               : VillagerRelationshipData.getGoldenRelationshipForPlayer(villager, player.getUUID(), familyVillager.getGoldenRelationship());
            if (!(goldenRelationship > 0.0F) && familyVillager.getRelationshipStage() != VillagerRelationshipStage.DATING) {
               return Collections.emptyMap();
            }

            return dialogueSet.datingDialogues().isEmpty() ? DATING_DIALOGUES : dialogueSet.datingDialogues();
         }

         if (familyVillager.getRelationshipStage() == VillagerRelationshipStage.DATING) {
            return dialogueSet.datingDialogues().isEmpty() ? DATING_DIALOGUES : dialogueSet.datingDialogues();
         }
      }

      return Collections.emptyMap();
   }

   private static Map<String, Map<String, Object>> getYoungStageDialogues(Villager villager) {
      return getYoungStageDialogues(villager, EMPTY_DIALOGUE_SET);
   }

   private static Map<String, Map<String, Object>> getYoungStageDialogues(Villager villager, DialogueManager.DialogueSet dialogueSet) {
      if (villager instanceof FamilyVillager familyVillager) {
         return switch (familyVillager.getAgeStage()) {
            case TODDLER -> !dialogueSet.toddlerDialogues().isEmpty()
               ? dialogueSet.toddlerDialogues()
               : (TODDLER_DIALOGUES.isEmpty() ? Collections.emptyMap() : TODDLER_DIALOGUES);
            case CHILD -> !dialogueSet.childDialogues().isEmpty()
               ? dialogueSet.childDialogues()
               : (CHILD_DIALOGUES.isEmpty() ? Collections.emptyMap() : CHILD_DIALOGUES);
            case TEEN -> !dialogueSet.teenDialogues().isEmpty()
               ? dialogueSet.teenDialogues()
               : (TEEN_DIALOGUES.isEmpty() ? Collections.emptyMap() : TEEN_DIALOGUES);
            case ADULT -> Collections.emptyMap();
         };
      } else {
         return Collections.emptyMap();
      }
   }

   private static String getPlayerChildPersonalityKey(Villager villager, Player player, String category, String personalityKey) {
      if (!"Intro".equalsIgnoreCase(category) || personalityKey == null || personalityKey.isEmpty()) {
         return personalityKey;
      } else {
         return isYoungVillager(villager) && isPlayerParent(villager, player) ? personalityKey + "_player_child" : personalityKey;
      }
   }

   private static boolean isYoungVillager(Villager villager) {
      if (!(villager instanceof FamilyVillager familyVillager)) {
         return false;
      } else {
         VillagerAgeStage ageStage = familyVillager.getAgeStage();
         return ageStage == VillagerAgeStage.TODDLER || ageStage == VillagerAgeStage.CHILD || ageStage == VillagerAgeStage.TEEN;
      }
   }

   private static boolean isPlayerParent(Villager villager, Player player) {
      if (villager instanceof FamilyVillager familyVillager && player instanceof ServerPlayer serverPlayer) {
         UUID parentPlayerUuid = familyVillager.getParentPlayerUuid();
         return parentPlayerUuid != null && parentPlayerUuid.equals(serverPlayer.getUUID());
      } else {
         return false;
      }
   }

   private static Object getDialogueBlock(Map<String, Object> byPersonality, String personalityKey, String fallbackPersonalityKey) {
      Object block = getIgnoreCase(byPersonality, personalityKey);
      if (block != null) {
         return block;
      }

      if (fallbackPersonalityKey != null && !fallbackPersonalityKey.equalsIgnoreCase(personalityKey)) {
         block = getIgnoreCase(byPersonality, fallbackPersonalityKey);
         if (block != null) {
            return block;
         }
      }

      return getIgnoreCase(byPersonality, "friendly");
   }

   private static <T> T getIgnoreCase(Map<String, T> map, String key) {
      if (map != null && !map.isEmpty() && key != null) {
         T exact = map.get(key);
         if (exact != null) {
            return exact;
         }

         for (Entry<String, T> entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(key)) {
               return entry.getValue();
            }
         }

         return null;
      } else {
         return null;
      }
   }

   private static VillagerPersonality getPersonality(Villager villager) {
      if (villager == null) {
         return null;
      } else if (villager instanceof MaleVillagerEntity male) {
         return male.getPersonality();
      } else {
         return villager instanceof FemaleVillagerEntity female ? female.getPersonality() : null;
      }
   }

   static {
      loadDialogues();
   }

   private record DialogueSet(
      Map<String, Map<String, Object>> dialogues,
      Map<String, Map<String, Object>> datingDialogues,
      Map<String, Map<String, Object>> marriedDialogues,
      Map<String, Map<String, Object>> toddlerDialogues,
      Map<String, Map<String, Object>> childDialogues,
      Map<String, Map<String, Object>> teenDialogues
   ) {
   }
}
