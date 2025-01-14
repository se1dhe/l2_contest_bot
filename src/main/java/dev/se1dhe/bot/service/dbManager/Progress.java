package dev.se1dhe.bot.service.dbManager;

public class Progress {
    private int objId;
    private int stageId;
    private int achId;
    private int value;

    // Конструктор, геттеры и сеттеры
    public Progress(int objId, int stageId, int achId, int value) {
        this.objId = objId;
        this.stageId = stageId;
        this.achId = achId;
        this.value = value;
    }

    public int getObjId() {
        return objId;
    }

    public void setObjId(int objId) {
        this.objId = objId;
    }

    public int getStageId() {
        return stageId;
    }

    public void setStageId(int stageId) {
        this.stageId = stageId;
    }

    public int getAchId() {
        return achId;
    }

    public void setAchId(int achId) {
        this.achId = achId;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}