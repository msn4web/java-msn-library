package net.sf.jml.message.p2p;

public class DisplayPictureDuelTimerTask extends java.util.TimerTask {

    private int baseId;
    private int lastStatus;
    private DisplayPictureDuelManager duelManager;

    public DisplayPictureDuelTimerTask(int baseId, 
    		                           int currentStatus,
    		                           DisplayPictureDuelManager duelManager) {
    	this.duelManager = duelManager;
        this.baseId = baseId;
        this.lastStatus = currentStatus;
    }

    @Override
	public void run() {
        try {
            DisplayPictureDuel d = duelManager.get(baseId);
            if (d != null && lastStatus == d.getDuelTimerStatus()) {
            	duelManager.remove(baseId);
            }
        } catch (RuntimeException e) {
        	e.printStackTrace();
        }
    }
}