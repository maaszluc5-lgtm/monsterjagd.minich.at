package at.minich.opserver;

import at.minich.opserver.chestshop.ChestShopListener;
import at.minich.opserver.chestshop.ChestShopManager;
import at.minich.opserver.commands.CrystalsCommand;
import at.minich.opserver.commands.GiveCrateCommand;
import at.minich.opserver.commands.GiveCrystalsCommand;
import at.minich.opserver.crates.CrateListener;
import at.minich.opserver.crates.CrateManager;
import at.minich.opserver.currency.CrystalManager;
import at.minich.opserver.ah.AuctionGUI;
import at.minich.opserver.ah.AuctionGUIListener;
import at.minich.opserver.ah.AuctionManager;
import at.minich.opserver.bank.BankGUI;
import at.minich.opserver.bank.BankGUIListener;
import at.minich.opserver.clan.ClanManager;
import at.minich.opserver.commands.*;
import at.minich.opserver.listeners.ClanChatListener;
import at.minich.opserver.listeners.DuelListener;
import at.minich.opserver.markt.*;
import at.minich.opserver.economy.*;
import at.minich.opserver.enchants.EnchantListener;
import at.minich.opserver.enchants.EnchantManager;
import at.minich.opserver.farmworld.FarmWorldManager;
import at.minich.opserver.jobs.JobGUI;
import at.minich.opserver.jobs.JobGUIListener;
import at.minich.opserver.jobs.JobListener;
import at.minich.opserver.jobs.JobManager;
import at.minich.opserver.kits.KitManager;
import at.minich.opserver.listeners.BackListener;
import at.minich.opserver.listeners.GodListener;
import at.minich.opserver.listeners.StatsListener;
import at.minich.opserver.listeners.TrashListener;
import at.minich.opserver.listeners.VanishListener;
import at.minich.opserver.mining.AreaMineListener;
import at.minich.opserver.mining.AreaMineManager;
import at.minich.opserver.items.AbilityItem;
import at.minich.opserver.items.AbilityItemListener;
import at.minich.opserver.ranks.RankManager;
import at.minich.opserver.rewards.DailyRewardManager;
import at.minich.opserver.salary.SalaryListener;
import at.minich.opserver.salaryfarm.SalaryFarmManager;
import at.minich.opserver.tasks.JackpotTask;
import at.minich.opserver.trade.TradeGUI;
import at.minich.opserver.trade.TradeGUIListener;
import at.minich.opserver.trophies.TrophyListener;
import at.minich.opserver.trophies.TrophyManager;
import at.minich.opserver.util.DataManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class OpServerPlugin extends JavaPlugin implements Listener {

    private DataManager dataManager;
    private EnchantManager enchantManager;
    private EconomyManager economyManager;
    private BankManager bankManager;
    private FarmWorldManager farmWorldManager;
    private SalaryFarmManager salaryFarmManager;
    private CoinsEconomy coinsEconomy;
    private RankManager rankManager;
    private DailyRewardManager dailyRewardManager;
    private KitManager kitManager;
    private JobManager jobManager;
    private TrophyManager trophyManager;
    private AreaMineManager areaMineManager;
    private MarktManager marktManager;
    private AuctionManager auctionManager;
    private ClanManager clanManager;
    private ChestShopManager chestShopManager;
    private JackpotTask jackpotTask;
    private CrystalManager crystalManager;
    private CrateManager crateManager;

    // Track login times to compute playtime on quit
    private final Map<UUID, Long> loginTimes = new HashMap<>();

    // Teleport / navigation state (in-memory)
    private final Map<UUID, UUID> tpaPendingRequests = new HashMap<>();
    private final Map<UUID, Integer> tpaExpiryTasks = new HashMap<>();
    private final Map<UUID, Location> backLocations = new HashMap<>();

    // Utility command state
    private final Set<UUID> godPlayers = new HashSet<>();
    private final Set<UUID> vanishPlayers = new HashSet<>();
    private final Set<Inventory> trashInventories = Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private final Map<UUID, UUID> lastMessagePartners = new HashMap<>();

    @Override
    public void onEnable() {
        // Save default config
        saveDefaultConfig();

        // Initialise managers
        dataManager = new DataManager(this);
        enchantManager = new EnchantManager(this);
        economyManager = new EconomyManager(dataManager);
        bankManager = new BankManager(dataManager);

        // Farm world (regular)
        String farmWorldName = getConfig().getString("farmworld.world-name", "farmworld");
        farmWorldManager = new FarmWorldManager(farmWorldName);
        farmWorldManager.initialize();

        // Salary farm world
        salaryFarmManager = new SalaryFarmManager(this);
        salaryFarmManager.initialize();

        // Rank, daily-reward, and kit managers (depend on economyManager/dataManager)
        rankManager = new RankManager(this);
        dailyRewardManager = new DailyRewardManager(this);
        kitManager = new KitManager(this);
        jobManager = new JobManager(this);
        trophyManager = new TrophyManager(this);
        areaMineManager = new AreaMineManager();
        marktManager = new MarktManager(this);
        auctionManager = new AuctionManager(this);
        clanManager = new ClanManager(this);
        chestShopManager = new ChestShopManager(this);
        crystalManager = new CrystalManager(this);
        crateManager = new CrateManager(this);

        // Register Vault economy
        registerVaultEconomy();

        // Register event listeners
        getServer().getPluginManager().registerEvents(new EnchantListener(this), this);
        getServer().getPluginManager().registerEvents(new StatsListener(this), this);
        getServer().getPluginManager().registerEvents(new SalaryListener(), this);
        getServer().getPluginManager().registerEvents(new AreaMineListener(this), this);
        getServer().getPluginManager().registerEvents(new BackListener(backLocations), this);
        getServer().getPluginManager().registerEvents(new GodListener(this), this);
        getServer().getPluginManager().registerEvents(new VanishListener(this), this);
        getServer().getPluginManager().registerEvents(new TrashListener(this), this);

        AbilityItemListener abilityItemListener = new AbilityItemListener(this);
        getServer().getPluginManager().registerEvents(abilityItemListener, this);
        getServer().getPluginManager().registerEvents(new at.minich.opserver.items.MagnetListener(this), this);

        // Duel listener (shared state with DuelCommand)
        DuelListener duelListener = new DuelListener(this);
        getServer().getPluginManager().registerEvents(duelListener, this);

        // Trade GUI listener
        java.util.Map<java.util.UUID, TradeGUI> activeTrades = new java.util.HashMap<>();
        getServer().getPluginManager().registerEvents(new TradeGUIListener(this, activeTrades), this);

        // Clan chat listener
        getServer().getPluginManager().registerEvents(new ClanChatListener(clanManager), this);

        // Chest shops
        getServer().getPluginManager().registerEvents(new ChestShopListener(this, chestShopManager), this);
        getServer().getPluginManager().registerEvents(new CrateListener(this), this);

        BankGUI bankGUI = new BankGUI(this);
        getServer().getPluginManager().registerEvents(new BankGUIListener(this, bankGUI), this);

        // Jobs
        JobGUI jobGUI = new JobGUI(this);
        getServer().getPluginManager().registerEvents(new JobListener(this), this);
        getServer().getPluginManager().registerEvents(new JobGUIListener(this, jobGUI), this);

        // Trophies
        getServer().getPluginManager().registerEvents(new TrophyListener(this), this);
        getServer().getPluginManager().registerEvents(new TrophyGUIListener(), this);

        // Markt
        MarktGUI marktGUI = new MarktGUI(this);
        MarktSellGUI marktSellGUI = new MarktSellGUI(this);
        MarktGUIListener marktGUIListener = new MarktGUIListener(this, marktGUI, marktSellGUI);
        getServer().getPluginManager().registerEvents(marktGUIListener, this);

        // Auktionshaus
        AuctionGUI auctionGUI = new AuctionGUI(this);
        AuctionGUIListener auctionGUIListener = new AuctionGUIListener(this, auctionGUI);
        getServer().getPluginManager().registerEvents(auctionGUIListener, this);

        getServer().getPluginManager().registerEvents(this, this);

        // Register commands
        getCommand("bal").setExecutor(new BalCommand(this));
        getCommand("baltop").setExecutor(new BaltopCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("bank").setExecutor(new BankCommand(this));
        getCommand("salary").setExecutor(new SalaryCommand(this));
        getCommand("lohn").setExecutor(new SalaryCommand(this));
        getCommand("giveitem").setExecutor(new GiveItemCommand(this));
        getCommand("giveabilityitem").setExecutor(new GiveAbilityItemCommand(this, abilityItemListener));
        getCommand("enchant").setExecutor(new EnchantCommand(this));
        getCommand("enchantlist").setExecutor(new EnchantListCommand(this));
        getCommand("farmworld").setExecutor(new FarmWorldCommand(this));
        getCommand("daily").setExecutor(new DailyCommand(this));
        getCommand("rank").setExecutor(new RankCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("kitlist").setExecutor(new KitListCommand(this));
        getCommand("stats").setExecutor(new StatsCommand(this));
        getCommand("lohnfarm").setExecutor(new LohnFarmCommand(this));
        getCommand("job").setExecutor(new JobCommand(this, jobGUI));
        getCommand("jobstats").setExecutor(new JobStatsCommand(this));
        getCommand("jobtop").setExecutor(new JobTopCommand(this));
        getCommand("trophies").setExecutor(new TrophyCommand(this));
        getCommand("markt").setExecutor(new MarktCommand(this, marktGUIListener));
        getCommand("ah").setExecutor(new AhCommand(this, auctionGUIListener));

        MiningCommand miningCommand = new MiningCommand(this);
        getCommand("mining").setExecutor(miningCommand);
        getCommand("mining").setTabCompleter(miningCommand);

        // Navigation commands
        HomeCommand homeCommand = new HomeCommand(this);
        getCommand("home").setExecutor(homeCommand);
        getCommand("sethome").setExecutor(homeCommand);
        getCommand("delhome").setExecutor(homeCommand);

        SpawnCommand spawnCommand = new SpawnCommand(this);
        getCommand("spawn").setExecutor(spawnCommand);
        getCommand("setspawn").setExecutor(spawnCommand);

        WarpCommand warpCommand = new WarpCommand(this);
        getCommand("warp").setExecutor(warpCommand);
        getCommand("setwarp").setExecutor(warpCommand);
        getCommand("delwarp").setExecutor(warpCommand);

        getCommand("pwarp").setExecutor(new PwarpCommand(this));

        TpaCommand tpaCommand = new TpaCommand(this, tpaPendingRequests, tpaExpiryTasks);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpaccept").setExecutor(new TpAcceptCommand(this, tpaPendingRequests, tpaExpiryTasks));
        getCommand("tpdeny").setExecutor(new TpDenyCommand(this, tpaPendingRequests, tpaExpiryTasks));

        getCommand("back").setExecutor(new BackCommand(this, backLocations));

        // Utility commands
        getCommand("repair").setExecutor(new RepairCommand(this));
        getCommand("hat").setExecutor(new HatCommand(this));
        getCommand("feed").setExecutor(new FeedCommand(this));
        getCommand("heal").setExecutor(new HealCommand(this));
        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("speed").setExecutor(new SpeedCommand(this));
        getCommand("god").setExecutor(new GodCommand(this));
        getCommand("vanish").setExecutor(new VanishCommand(this));
        getCommand("nick").setExecutor(new NickCommand(this));
        getCommand("seen").setExecutor(new SeenCommand(this));
        getCommand("ping").setExecutor(new PingCommand(this));
        getCommand("trash").setExecutor(new TrashCommand(this));
        MsgCommand msgCommand = new MsgCommand(this);
        getCommand("msg").setExecutor(msgCommand);
        getCommand("r").setExecutor(new ReplyCommand(this));

        // Fun/social commands
        getCommand("cf").setExecutor(new CfCommand(this));

        jackpotTask = new JackpotTask(this);
        jackpotTask.runTaskTimer(this, 20L * 60 * 5, 20L * 60 * 5); // every 5 minutes
        getCommand("jackpot").setExecutor(new JackpotCommand(this, jackpotTask));

        getCommand("duel").setExecutor(new DuelCommand(this, duelListener));

        getCommand("trade").setExecutor(new TradeCommand(this, activeTrades));

        getCommand("report").setExecutor(new ReportCommand(this));
        getCommand("helpop").setExecutor(new HelpopCommand(this));

        ClanCommand clanCommand = new ClanCommand(this, clanManager);
        getCommand("clan").setExecutor(clanCommand);
        getCommand("cc").setExecutor(clanCommand);

        getCommand("kristalle").setExecutor(new CrystalsCommand(this));
        getCommand("givekristalle").setExecutor(new GiveCrystalsCommand(this));
        getCommand("givecrate").setExecutor(new GiveCrateCommand(this));

        // Scheduled tasks
        double coinsPerMinute = getConfig().getDouble("salary.coins-per-minute", 10.0);
        new SalaryTask(economyManager, rankManager, dataManager, coinsPerMinute)
                .runTaskTimer(this, 20L * 60, 20L * 60); // every 60 seconds

        double interestRate = getConfig().getDouble("bank.interest-rate-percent", 1.0);
        new BankInterestTask(bankManager, interestRate)
                .runTaskTimer(this, 20L * 3600, 20L * 3600); // every hour

        // Auction finalization — every 30 seconds
        getServer().getScheduler().runTaskTimer(this, () -> auctionManager.finalizeAuctions(), 20L * 30, 20L * 30);

        getLogger().info("OpServer plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (economyManager != null) economyManager.save();
        if (bankManager != null) bankManager.save();
        if (clanManager != null) clanManager.save();
        if (chestShopManager != null) chestShopManager.save();
        savePlayerData();
        getLogger().info("OpServer plugin disabled. Data saved.");
    }

    // -------------------------------------------------------------------------
    // Player lifecycle events
    // -------------------------------------------------------------------------

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Track login time for playtime calculation
        loginTimes.put(uuid, System.currentTimeMillis());

        // Init economy accounts for new players
        economyManager.initPlayer(uuid);
        bankManager.initPlayer(uuid);

        // Load/create player data file
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        if (!cfg.contains("join-date")) {
            cfg.set("join-date", Instant.now().toString());
            cfg.set("name", player.getName());
            dataManager.saveYaml(cfg, path);
        }
        // Update name in case it changed
        cfg.set("name", player.getName());
        cfg.set("last-seen", Instant.now().toString());
        dataManager.saveYaml(cfg, path);

        // Apply rank prefix
        rankManager.applyRankPrefix(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Persist economy data on quit
        economyManager.save();
        bankManager.save();

        // Accumulate playtime
        String path = "players/" + uuid + ".yml";
        YamlConfiguration cfg = dataManager.loadYaml(path);
        long loginTime = loginTimes.getOrDefault(uuid, System.currentTimeMillis());
        long sessionSeconds = (System.currentTimeMillis() - loginTime) / 1000;
        long currentPlaytime = cfg.getLong("playtime-seconds", 0);
        cfg.set("playtime-seconds", currentPlaytime + sessionSeconds);
        cfg.set("last-seen", Instant.now().toString());
        dataManager.saveYaml(cfg, path);

        loginTimes.remove(uuid);

        // Free area mine data (resets on logout by design)
        areaMineManager.remove(uuid);
    }

    // -------------------------------------------------------------------------
    // Vault registration
    // -------------------------------------------------------------------------

    private void registerVaultEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found! Economy features may not work with other plugins.");
            return;
        }
        coinsEconomy = new CoinsEconomy(economyManager);
        getServer().getServicesManager().register(Economy.class, coinsEconomy, this, ServicePriority.Highest);
        getLogger().info("CoinsEconomy registered with Vault.");
    }

    // -------------------------------------------------------------------------
    // Save all player data
    // -------------------------------------------------------------------------

    private void savePlayerData() {
        dataManager.ensureDir("players");
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            String path = "players/" + uuid + ".yml";
            YamlConfiguration cfg = dataManager.loadYaml(path);
            cfg.set("last-seen", Instant.now().toString());
            cfg.set("name", player.getName());

            // Flush playtime for online players on shutdown
            long loginTime = loginTimes.getOrDefault(uuid, now);
            long sessionSeconds = (now - loginTime) / 1000;
            long currentPlaytime = cfg.getLong("playtime-seconds", 0);
            cfg.set("playtime-seconds", currentPlaytime + sessionSeconds);

            dataManager.saveYaml(cfg, path);
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public DataManager getDataManager() {
        return dataManager;
    }

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public BankManager getBankManager() {
        return bankManager;
    }

    public FarmWorldManager getFarmWorldManager() {
        return farmWorldManager;
    }

    public SalaryFarmManager getSalaryFarmManager() {
        return salaryFarmManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public DailyRewardManager getDailyRewardManager() {
        return dailyRewardManager;
    }

    public KitManager getKitManager() {
        return kitManager;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public TrophyManager getTrophyManager() {
        return trophyManager;
    }

    public AreaMineManager getAreaMineManager() {
        return areaMineManager;
    }

    public MarktManager getMarktManager() {
        return marktManager;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public ChestShopManager getChestShopManager() {
        return chestShopManager;
    }

    public JackpotTask getJackpotTask() {
        return jackpotTask;
    }

    public CrystalManager getCrystalManager() {
        return crystalManager;
    }

    public CrateManager getCrateManager() {
        return crateManager;
    }

    // -------------------------------------------------------------------------
    // God mode
    // -------------------------------------------------------------------------

    public boolean toggleGodMode(UUID uuid) {
        if (godPlayers.contains(uuid)) {
            godPlayers.remove(uuid);
            return false;
        } else {
            godPlayers.add(uuid);
            return true;
        }
    }

    public boolean isGodMode(UUID uuid) {
        return godPlayers.contains(uuid);
    }

    // -------------------------------------------------------------------------
    // Vanish
    // -------------------------------------------------------------------------

    public boolean toggleVanish(Player player) {
        UUID uuid = player.getUniqueId();
        if (vanishPlayers.contains(uuid)) {
            vanishPlayers.remove(uuid);
            // Show player to everyone
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.showPlayer(this, player);
            }
            return false;
        } else {
            vanishPlayers.add(uuid);
            // Hide player from non-ops
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player) && !online.isOp() && !online.hasPermission("opserver.admin")) {
                    online.hidePlayer(this, player);
                }
            }
            return true;
        }
    }

    public boolean isVanished(UUID uuid) {
        return vanishPlayers.contains(uuid);
    }

    // -------------------------------------------------------------------------
    // Trash
    // -------------------------------------------------------------------------

    public void registerTrashInventory(Inventory inventory) {
        trashInventories.add(inventory);
    }

    public boolean isTrashInventory(Inventory inventory) {
        return trashInventories.contains(inventory);
    }

    public void unregisterTrashInventory(Inventory inventory) {
        trashInventories.remove(inventory);
    }

    // -------------------------------------------------------------------------
    // Private messages
    // -------------------------------------------------------------------------

    public void setLastMessagePartner(UUID player, UUID partner) {
        if (partner == null) {
            lastMessagePartners.remove(player);
        } else {
            lastMessagePartners.put(player, partner);
        }
    }

    public UUID getLastMessagePartner(UUID player) {
        return lastMessagePartners.get(player);
    }
}
