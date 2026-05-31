package tetris.model;

public class GameConfig {

    private double bgmVolume  = 1.0;
    private double seVolume   = 0.70;
    private boolean bgmEnabled = true;
    private boolean seEnabled  = true;

    public double getBgmVolume()        { return bgmVolume; }
    public void   setBgmVolume(double v){ bgmVolume = v; }

    public double getSeVolume()         { return seVolume; }
    public void   setSeVolume(double v) { seVolume = v; }

    public boolean isBgmEnabled()         { return bgmEnabled; }
    public void    setBgmEnabled(boolean b){ bgmEnabled = b; }

    public boolean isSeEnabled()          { return seEnabled; }
    public void    setSeEnabled(boolean b) { seEnabled = b; }
}
