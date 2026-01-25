package me.hasenzahn1.pvp.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import org.bukkit.event.player.PlayerItemDamageEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {

    private ConnectionSource connectionSource;

    private Dao<PlayerStateEntry, UUID> playerStateDao;
    private Dao<PlayerDeathEntry, UUID> playerDeathDao;
    private Dao<PlayerDamageEntry, UUID> playerDamageDao;

    private final HashMap<UUID, PlayerStateEntry> playerStates = new HashMap<>();
    private final ArrayList<PlayerDamageEntry> damageEntryQueue = new ArrayList<>();


    public void connect(String databasePath) throws SQLException {
        String url = "jdbc:sqlite:" + databasePath;
        connectionSource = new JdbcConnectionSource(url);

        playerStateDao = DaoManager.createDao(connectionSource, PlayerStateEntry.class);
        playerDeathDao = DaoManager.createDao(connectionSource, PlayerDeathEntry.class);
        playerDamageDao = DaoManager.createDao(connectionSource, PlayerDamageEntry.class);

        createTables();
    }

    private void createTables() throws SQLException {
        TableUtils.createTableIfNotExists(connectionSource, PlayerStateEntry.class);
        TableUtils.createTableIfNotExists(connectionSource, PlayerDeathEntry.class);
        TableUtils.createTableIfNotExists(connectionSource, PlayerDamageEntry.class);
    }

    public void disconnect(){
        for(PlayerStateEntry state : playerStates.values()){
            state.update();
        }

        flushAllDamageEntriesToDatabase();

        playerStates.clear();

        if(connectionSource != null) {
            connectionSource.closeQuietly();
        }
    }

    public void addDamageEntry(PlayerDamageEntry damageEntry){
        damageEntryQueue.add(damageEntry);

        if(damageEntryQueue.size() > 10){
            flushAllDamageEntriesToDatabase();
        }
    }

    private void flushAllDamageEntriesToDatabase(){
        if(damageEntryQueue.isEmpty()){
            return;
        }
        try {
            playerDamageDao.callBatchTasks(() -> {
                for(PlayerDamageEntry entry : damageEntryQueue){
                    playerDamageDao.create(entry);
                }
                return null;
            });

            damageEntryQueue.clear();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error saving the entries!");
        }
    }

    public Dao<PlayerStateEntry, UUID> getPlayerStateDao() {
        return playerStateDao;
    }

    public Dao<PlayerDeathEntry, UUID> getPlayerDeathDao() {
        return playerDeathDao;
    }

    public HashMap<UUID, PlayerStateEntry> getPlayerStates(){
        return playerStates;
    }

    /**
     * Get all death entries for a player, ordered by timestamp (most recent first)
     */
    public List<PlayerDeathEntry> getDeathsForPlayer(UUID uuid) {
        try {
            List<PlayerDeathEntry> deaths = playerDeathDao.queryBuilder()
                    .orderBy("timestamp", false)
                    .where()
                    .eq("uuid", uuid)
                    .query();
            return deaths != null ? deaths : Collections.emptyList();
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Get a specific death entry by its ID
     */
    public PlayerDeathEntry getDeathById(int id) {
        try {
            return playerDeathDao.queryBuilder()
                    .where()
                    .eq("id", id)
                    .queryForFirst();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
