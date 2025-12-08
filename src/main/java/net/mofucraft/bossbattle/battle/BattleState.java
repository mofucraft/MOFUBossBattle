package net.mofucraft.bossbattle.battle;

public enum BattleState {
    WAITING,           // Player teleported, waiting to start
    IN_PROGRESS,       // Battle active
    ITEM_COLLECTION,   // Boss defeated, collecting items
    COMPLETED,         // Battle finished successfully
    FAILED             // Player died/logged out/timeout
}
