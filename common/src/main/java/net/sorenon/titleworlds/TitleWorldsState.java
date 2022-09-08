package net.sorenon.titleworlds;

public class TitleWorldsState {
    public static TitleWorldsState STATE = new TitleWorldsState();

    public boolean isTitleWorld = false;

    //TODO we need to figure out a different way to pause the game because this prevents syncing
    public boolean pause = false;
    public boolean noSave = true;
    public boolean reloading = false;
    public int neededRadiusCenterInclusive = 0;
}
