package me.hasenzahn1.pvp.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import me.hasenzahn1.pvp.PvpSystem;

import java.sql.SQLException;
import java.util.UUID;

@DatabaseTable(tableName = "modeswitch")
public class PlayerModeSwitchEntry {

    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField
    public UUID uuid;

    @DatabaseField
    public long timestamp;

    @DatabaseField
    public boolean mode;

    public PlayerModeSwitchEntry() {}

    public PlayerModeSwitchEntry(UUID uuid, boolean mode) {
        this.uuid = uuid;
        this.mode = mode;
        timestamp = System.currentTimeMillis();
    }

    public void update(){
        try {
            PvpSystem.getInstance().getDatabase().getPlayerModeSwitchDao().update(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(){
        try {
            PvpSystem.getInstance().getDatabase().getPlayerModeSwitchDao().create(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getId() {
        return id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isMode() {
        return mode;
    }
}
