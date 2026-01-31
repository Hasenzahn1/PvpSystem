package me.hasenzahn1.pvp.database;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import me.hasenzahn1.pvp.commands.lookup.LookupEntry;
import me.hasenzahn1.pvp.commands.lookup.LookupFilter;
import org.bukkit.Location;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

    public void flushAllDamageEntriesToDatabase(){
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
            System.err.println("Fehler beim Batch-Speichern der Damage-Einträge!");
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

    public Dao<PlayerDamageEntry, UUID> getPlayerDamageDao() {
        return playerDamageDao;
    }

    /**
     * Query entries (deaths and/or damage) based on filter criteria
     */
    public List<LookupEntry> queryLookupEntries(LookupFilter filter, Location playerLocation) {
        List<LookupEntry> results = new ArrayList<>();

        // Query deaths if type is null or DEATH
        if (filter.getType() == null || filter.getType() == LookupFilter.EntryType.DEATH) {
            List<PlayerDeathEntry> deaths = queryDeathsWithFilter(filter);
            for (PlayerDeathEntry death : deaths) {
                // Apply radius filter if set
                if (filter.getRadius() != null && filter.getRadius() > 0 && playerLocation != null) {
                    if (!isWithinRadius(playerLocation, death.getWorld(), death.getX(), death.getY(), death.getZ(), filter.getRadius())) {
                        continue;
                    }
                }
                results.add(new LookupEntry(death));
            }
        }

        // Query damage if type is null or DAMAGE
        if (filter.getType() == null || filter.getType() == LookupFilter.EntryType.DAMAGE) {
            List<PlayerDamageEntry> damages = queryDamagesWithFilter(filter);
            for (PlayerDamageEntry damage : damages) {
                // Apply radius filter if set
                if (filter.getRadius() != null && filter.getRadius() > 0 && playerLocation != null) {
                    if (!isWithinRadius(playerLocation, damage.getWorld(), damage.getX(), damage.getY(), damage.getZ(), filter.getRadius())) {
                        continue;
                    }
                }
                results.add(new LookupEntry(damage));
            }
        }

        // Sort by timestamp descending (most recent first)
        results.sort(Comparator.comparingLong(LookupEntry::getTimestamp).reversed());

        return results;
    }

    private boolean isWithinRadius(Location playerLoc, String world, double x, double y, double z, int radius) {
        if (!playerLoc.getWorld().getName().equals(world)) {
            return false;
        }
        double dx = playerLoc.getX() - x;
        double dy = playerLoc.getY() - y;
        double dz = playerLoc.getZ() - z;
        return (dx * dx + dy * dy + dz * dz) <= (radius * radius);
    }

    private List<PlayerDeathEntry> queryDeathsWithFilter(LookupFilter filter) {
        try {
            QueryBuilder<PlayerDeathEntry, UUID> queryBuilder = playerDeathDao.queryBuilder();
            queryBuilder.orderBy("timestamp", false);

            Where<PlayerDeathEntry, UUID> where = queryBuilder.where();
            boolean hasCondition = false;

            if (filter.getPlayerUuid() != null) {
                if (hasCondition) where.and();
                where.eq("uuid", filter.getPlayerUuid());
                hasCondition = true;
            }

            if (filter.getAttacker() != null) {
                if (hasCondition) where.and();
                where.eq("attacker", filter.getAttacker());
                hasCondition = true;
            }

            if (filter.getCause() != null) {
                if (hasCondition) where.and();
                where.eq("cause", filter.getCause());
                hasCondition = true;
            }

            if (filter.getWorld() != null) {
                if (hasCondition) where.and();
                where.eq("world", filter.getWorld());
                hasCondition = true;
            }

            if (filter.getMode() != null) {
                if (hasCondition) where.and();
                where.eq("defenderMode", filter.getMode());
                hasCondition = true;
            }

            if (filter.getTimeThreshold() != null) {
                if (hasCondition) where.and();
                where.ge("timestamp", filter.getTimeThreshold());
                hasCondition = true;
            }

            if (filter.getTimeUpperBound() != null) {
                if (hasCondition) where.and();
                where.le("timestamp", filter.getTimeUpperBound());
                hasCondition = true;
            }

            if (!hasCondition) {
                return playerDeathDao.queryBuilder()
                        .orderBy("timestamp", false)
                        .query();
            }

            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private List<PlayerDamageEntry> queryDamagesWithFilter(LookupFilter filter) {
        // Flush pending damage entries first to ensure we query all data
        flushAllDamageEntriesToDatabase();

        try {
            QueryBuilder<PlayerDamageEntry, UUID> queryBuilder = playerDamageDao.queryBuilder();
            queryBuilder.orderBy("timestamp", false);

            Where<PlayerDamageEntry, UUID> where = queryBuilder.where();
            boolean hasCondition = false;

            if (filter.getPlayerUuid() != null) {
                if (hasCondition) where.and();
                where.eq("uuid", filter.getPlayerUuid());
                hasCondition = true;
            }

            if (filter.getAttacker() != null) {
                if (hasCondition) where.and();
                where.eq("attacker", filter.getAttacker());
                hasCondition = true;
            }

            if (filter.getCause() != null) {
                if (hasCondition) where.and();
                where.eq("cause", filter.getCause());
                hasCondition = true;
            }

            if (filter.getWorld() != null) {
                if (hasCondition) where.and();
                where.eq("world", filter.getWorld());
                hasCondition = true;
            }

            if (filter.getMode() != null) {
                if (hasCondition) where.and();
                where.eq("defenderMode", filter.getMode());
                hasCondition = true;
            }

            if (filter.getTimeThreshold() != null) {
                if (hasCondition) where.and();
                where.ge("timestamp", filter.getTimeThreshold());
                hasCondition = true;
            }

            if (filter.getTimeUpperBound() != null) {
                if (hasCondition) where.and();
                where.le("timestamp", filter.getTimeUpperBound());
                hasCondition = true;
            }

            if (!hasCondition) {
                return playerDamageDao.queryBuilder()
                        .orderBy("timestamp", false)
                        .query();
            }

            return queryBuilder.query();
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }
}
