package me.hasenzahn1.pvp.database;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import me.hasenzahn1.pvp.PvpSystem;

import java.sql.SQLException;
import java.util.UUID;

@DatabaseTable(tableName = "playerstates")
public class PlayerStateEntry {

    @DatabaseField(id = true)
    public UUID uuid;

    @DatabaseField
    public boolean state;

    @DatabaseField
    public boolean forcePeaceful = false;

    @DatabaseField
    public boolean disablePVP = false;

    public PlayerStateEntry(){}

    public PlayerStateEntry(UUID uuid, boolean state, boolean forcePeaceful, boolean disablePVP) {
        this.uuid = uuid;
        this.state = state;
        this.forcePeaceful = forcePeaceful;
        this.disablePVP = disablePVP;
    }

    public static PlayerStateEntry queryForId(UUID uuid){
        try{
            PlayerStateEntry pState = PvpSystem.getInstance().getDatabase().getPlayerStateDao().queryForId(uuid);
            System.out.println("State: " + pState);
            if(pState == null) {
                pState = new PlayerStateEntry(uuid, true, false, false);
                PvpSystem.getInstance().getDatabase().getPlayerStateDao().create(pState);
            }
            return pState;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static PlayerStateEntry queryForIdWithoutCreation(UUID uuid){
        PvpSystem.getInstance().getDatabase().updateAllPlayerStates();
        try{
            return PvpSystem.getInstance().getDatabase().getPlayerStateDao().queryForId(uuid);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void update(){
        try {
            PvpSystem.getInstance().getDatabase().getPlayerStateDao().update(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void create(){
        try {
            PvpSystem.getInstance().getDatabase().getPlayerStateDao().create(this);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
