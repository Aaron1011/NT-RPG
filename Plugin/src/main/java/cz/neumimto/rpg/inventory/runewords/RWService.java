package cz.neumimto.rpg.inventory.runewords;

import cz.neumimto.core.ioc.Inject;
import cz.neumimto.core.ioc.PostProcess;
import cz.neumimto.core.ioc.Singleton;
import cz.neumimto.rpg.GroupService;
import cz.neumimto.rpg.NtRpgPlugin;
import cz.neumimto.rpg.Pair;
import cz.neumimto.rpg.TextHelper;
import cz.neumimto.rpg.configuration.Localization;
import cz.neumimto.rpg.configuration.PluginConfig;
import cz.neumimto.rpg.effects.EffectService;
import cz.neumimto.rpg.events.RebuildRunewordEvent;
import cz.neumimto.rpg.inventory.InventoryService;
import cz.neumimto.rpg.inventory.SocketType;
import cz.neumimto.rpg.inventory.data.ItemSocket;
import cz.neumimto.rpg.inventory.data.NKeys;
import cz.neumimto.rpg.inventory.data.manipulators.ItemSocketsData;
import cz.neumimto.rpg.inventory.data.manipulators.ItemStackUpgradeData;
import cz.neumimto.rpg.utils.ItemStackUtils;
import cz.neumimto.rpg.utils.Utils;
import cz.neumimto.rpg.utils.XORShiftRnd;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.item.ItemType;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by NeumimTo on 29.10.2015.
 */
@Singleton
public class RWService {

	private final Pattern socket = Pattern.compile("\\{@\\}");
	private final Path file = Paths.get(NtRpgPlugin.workingDir, "Runes.conf");

	@Inject
	private RWDao dao;

	@Inject
	private EffectService effectService;

	@Inject
	private Logger logger;

	@Inject
	private Game game;

	@Inject
	private InventoryService inventoryService;

	@Inject
	private GroupService groupService;

	private Map<String, RuneWord> runewords = new HashMap<>();
	private Map<String, RuneWord> combinations = new HashMap<>();
	private Map<String, Rune> runes = new HashMap<>();
	private List<ItemType> allowedRuneItemTypes = new ArrayList<>();


	@PostProcess(priority = 10000)
	public void load() {
		File p = file.toFile();
		if (!p.exists()) {
			try {
				p.createNewFile();
				Files.write(p.toPath(), "Runes:{},\nRuneWords:[]".getBytes());
				return;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		for (Rune rune : dao.getAllRunes(p)) {
			runes.put(rune.getName().toLowerCase(), rune);
		}
		Set<RuneWordTemplate> allRws = dao.getAllRws(p);
		for (RuneWordTemplate runeWord : allRws) {
			registerRuneword(getRuneword(runeWord));
		}
		for (String s : PluginConfig.ALLOWED_RUNES_ITEMTYPES) {
			Optional<ItemType> type = Sponge.getGame().getRegistry().getType(ItemType.class, s);
			type.ifPresent(itemType -> allowedRuneItemTypes.add(itemType));
		}
	}

	protected RuneWord getRuneword(RuneWordTemplate template) {
		template.getEffects().keySet().stream().filter(Utils.not(effectService::isGlobalEffect)).forEach(e -> logger.warn("Runeword " + template + " defined non existing global effect:" + e));
		RuneWord rw = new RuneWord();
		rw.setName(template.getName());
		rw.setRunes(template.getRunes()
				.stream()/*filter(this::existsRune).*/
				.map(this::getRune)
				.collect(Collectors.toList()));
		rw.setMinLevel(template.getMinLevel());

		rw.setAllowedItems(template.getAllowedItems()
				.stream()
				.map(a -> game.getRegistry().getType(ItemType.class, a))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toSet()));

		rw.setEffects(template.getEffects().entrySet().stream()
				.filter(l -> effectService.isGlobalEffect(l.getKey()))
				.map(a -> new Pair<>(effectService.getGlobalEffect(a.getKey()), a.getValue()))
				.collect(HashMap::new, (map, a) -> map.put(a.key, a.value), HashMap::putAll));

		return rw;
	}

	public RuneWord getRuneword(String name) {
		return runewords.get(name.toLowerCase());
	}

	public int getSocketCount(ItemStack itemStack) {
		Optional<List<Text>> itemSockets = itemStack.get(NKeys.ITEM_SOCKET_CONTAINER);
		return itemSockets.map(List::size).orElse(0);
	}

	public boolean hasEmptySocket(ItemStack itemStack) {
		Optional<List<Text>> itemSockets = itemStack.get(NKeys.ITEM_SOCKET_CONTAINER);
		if (!itemSockets.isPresent())
			return false;
		List<Text> itemSockets1 = itemSockets.get();
		for (Text itemSocket : itemSockets1) {
			if (itemSocket.toPlain().equalsIgnoreCase(Localization.SOCKET_EMPTY)) {
				return true;
			}
		}
		return false;
	}

	public boolean existsRune(String rune) {
		return runes.containsKey(rune.toLowerCase());
	}

	public boolean existsRuneword(String rw) {
		return runewords.containsKey(rw.toLowerCase());
	}

	public ItemUpgrade getRune(String rune) {
		return runes.get(rune.toLowerCase());
	}

	public Map<String, Rune> getRunes() {
		return runes;
	}

	public ItemStack createRune(SocketType r, String name) {
		ItemUpgrade rune = getRune(name);
		if (rune == null)
			return ItemStack.empty();
		XORShiftRnd rnd = new XORShiftRnd();
		int i = rnd.nextInt(allowedRuneItemTypes.size());
		ItemType itemType = allowedRuneItemTypes.get(i);
		ItemStack of = ItemStack.of(itemType,1);
		of.offer(Keys.HIDE_ATTRIBUTES, true);
		of.offer(Keys.HIDE_MISCELLANEOUS, true);
		of.offer(new ItemStackUpgradeData(r));
		of.offer(Keys.DISPLAY_NAME, TextHelper.parse(rune.getName()));
		return of;
	}


	public ItemStack createSocket(ItemStack itemStack, SocketType type) {
		Optional<ItemSocketsData> opt = itemStack.getOrCreate(ItemSocketsData.class);
		ItemSocketsData socketsData = opt.get();

		socketsData.getSockets().add(type);
		socketsData.getContent().add(TextHelper.parse(Localization.SOCKET_EMPTY));

		itemStack.offer(socketsData);

		inventoryService.updateLore(itemStack);
		return itemStack;
	}

	public ItemStack insertRune(ItemStack itemStack, ItemStack rune) {
		Optional<ItemSocketsData> itemSocketsData = itemStack.get(ItemSocketsData.class);
		if (itemSocketsData.isPresent()) {
			return itemStack;
		}
		ItemSocketsData socketsData = itemSocketsData.get();


		Optional<List<ItemSocket>> opt = itemStack.get(NKeys.ITEM_SOCKET_CONTAINER);
		if (opt.isPresent()) {
			List<ItemSocket> itemSockets = opt.get();
			for (ItemSocket itemSocket : itemSockets) {
				if (itemSocket.getContent() == null && itemSocket.getType() == SocketType.ANY || itemSocket.getType() == SocketType.RUNE) {

					itemStack.offer(NKeys.ITEM_SOCKET_CONTAINER, itemSockets);
					break;
				}
			}
		} else {
			return itemStack;
		}

		return itemStack;
	}

	public String getCurrentRuneCombination(ItemStack i) {
		Optional<List<ItemSocket>> itemSockets = i.get(NKeys.ITEM_SOCKET_CONTAINER);
		if (!itemSockets.isPresent())
			return null;
		List<ItemSocket> sockets = itemSockets.get();
		StringBuilder builder = new StringBuilder();
		for (ItemSocket itemSocket : sockets) {
			if (itemSocket.getType() != SocketType.RUNE || itemSocket.getType() != SocketType.ANY || itemSocket.getContent() == null) {
				builder = null;
				break;
			}
			builder.append(itemSocket.getContent().getName());
		}
		return builder != null ? builder.toString() : null;
	}

	public ItemStack findRuneword(ItemStack i) {
		String combination = getCurrentRuneCombination(i);
		if (combination == null)
			return i;
			RuneWord rw = combinations.get(combination);
			if (rw != null) {
				if (rw.getAllowedItems().contains(i.getType())) {
					if (rw.getAllowedItems().isEmpty()) {
						return reBuildRuneword(i, rw);
					} else {
						if (rw.getAllowedItems().contains(i.getType())) {
							return reBuildRuneword(i, rw);
						}
					}
				}
			}
		return i;
	}

	public ItemStack reBuildRuneword(ItemStack i, RuneWord rw) {
		if (rw == null) {
			if (PluginConfig.AUTOREMOVE_NONEXISTING_RUNEWORDS) {
				i.offer(Keys.DISPLAY_NAME, Text.of(i.getType().getName()));
				i.offer(Keys.ITEM_LORE, Collections.<Text>emptyList());
			}
			return i;
		}
		i.offer(Keys.HIDE_ATTRIBUTES, true);
		i.offer(Keys.HIDE_MISCELLANEOUS, true);
		inventoryService.updateLore(i);
		RebuildRunewordEvent event = new RebuildRunewordEvent(rw, i);
		Sponge.getEventManager().post(event);
		i = event.getItemStack();
		return i;
	}


	public boolean canBeInserted(RuneWordTemplate template, ItemStack itemStack) {
		List<String> allowedItems = template.getAllowedItems();
		ItemType i = itemStack.getType();
		if (allowedItems.contains(itemStack.toString())) {
			return true;
		}
		for (String allowedItem : allowedItems) {
			if (allowedItem.startsWith("any")) {
				String[] split = allowedItem.split(" ");
				if (split.length == 1) {
					return true;
				}
				if (split.length == 2) {
					return ItemStackUtils.checkType(i, split[1]);
				}

			}
		}

		return false;
	}

	public List<ItemType> getAllowedRuneItemTypes() {
		return allowedRuneItemTypes;
	}

	public RuneWord findByCombination(String runes) {
		return combinations.get(runes);
	}

	public void registerRuneword(RuneWord runeWord) {
		runewords.put(runeWord.getName(), runeWord);
		if (runeWord.getRunes() != null && !runeWord.getRunes().isEmpty()) {
			combinations.put(runeWord.getRunes().stream().map(ItemUpgrade::getName).collect(Collectors.joining("")), runeWord);
		}
	}

	public Map<String, RuneWord> getRunewords() {
		return runewords;
	}

	public boolean isRune(ItemStack rune) {
		Optional<Text> text = rune.get(NKeys.ITEM_TYPE);
		if (text.isPresent()) {
			String s = text.get().toPlain().toLowerCase();
			for (SocketType type : SocketType.values()) {
				if (s.contains(type.name().toLowerCase()))
					return true;
			}

		}
		return false;
	}

}
