package net.sf.jml.message.p2p;

import net.sf.jml.MsnObject;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is the manager for all MsnObjects P2P transmisions from this 
 * client to a remote one.
 *   
 * @author Angel Barragán Chacón
 */
public class DisplayPictureDuelManager {

	/**
	 * Executor thread.
	 */
	private ScheduledExecutorService THREAD_EXECUTOR;
	
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Create a new instance of the duel manager.
	 */
	public DisplayPictureDuelManager() {
		
		// Initialize the list of duels
		duels = new ArrayList<DisplayPictureDuel>();
		
		// Initilize list of pictures
		pictures = new Hashtable<String, RemovableMsnObject>();
        
        THREAD_EXECUTOR = Executors.newScheduledThreadPool(1);

		// Initialize the executor thread
		THREAD_EXECUTOR.scheduleWithFixedDelay(
				new RemoveMsnObjectWorker(), 
				10*60,
				10*60, 
				TimeUnit.SECONDS);
	}

    public void stop()
    {
        if(THREAD_EXECUTOR != null)
            THREAD_EXECUTOR.shutdownNow();
    }

	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * List with the current works to return a MsnObject.
	 */
	private List<DisplayPictureDuel> duels;

	/**
	 * Add a new instance of a DisplayPictureDuel to the list of workers.
	 * 
	 * @param duel Instance of the worker.
	 */
	public synchronized void add(DisplayPictureDuel duel) {
		duels.add(duel);
	}

	/**
	 * Get an instance of a DisplayPictureDuel worker based on it's ID.
	 *  
	 * @param baseId Id of the worker.
	 * @return instance of the worker.
	 */
	public synchronized DisplayPictureDuel get(int baseId) {
		for(DisplayPictureDuel item : duels) {
			if (item.getBaseId() == baseId) {
				return item;
			}
		}
		return null;
	}

	/**
	 * Removes a DisplayPictureDuel based on the worker id.
	 * 
	 * @param baseId Id of the worker.
	 * @return True if it was removed and false if not.
	 */
	public synchronized boolean remove(int baseId) {
		DisplayPictureDuel duel = get(baseId);
		return duels.remove(duel);
	}

	/**
	 * Get the number of actual workers.
	 * 
	 * @return Number of actual workers.
	 */
	public int getSize() {
		return duels.size();
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Hashtable of pictures.
	 */
	private Hashtable<String, RemovableMsnObject> pictures;

	/**
	 * Add a new picture to be available to be sended when required.
	 * 
	 * @param key Key for the picture.
	 * @param obj Instance of the MsnObject.
	 */
	public void putPicture(String key, MsnObject obj) {
		RemovableMsnObject msnObj = new RemovableMsnObject(obj);
		pictures.put(key, msnObj);
	}

	/**
	 * Get a picture based on it's key.
	 * 
	 * @param key Key of the picture.
	 * @return Instance of the picture.
	 */
	public MsnObject getPicture(String key) {
		RemovableMsnObject ret = pictures.get(key);
		if (ret != null) {
			return ret.getMsnObject();
		}
		return getDisplayPicture();
	}

	////////////////////////////////////////////////////////////////////////////
	
	////////////////////////////////////////////////////////////////////////////
	
	/**
	 * Current user display picture.
	 */
	private MsnObject displayPicture;

	/**
	 * Retrieves the current user display picture.
	 * 
	 * @return Instance of the current user display pìcture.
	 */
	public MsnObject getDisplayPicture() {
		return displayPicture;
	}

	/**
	 * Sets the current user display picture.
	 * 
	 * @param displayPicture Instance of the current user display picture.
	 */
	public void setDisplayPicutre(MsnObject displayPicture) {
		this.displayPicture = displayPicture;
	}

	////////////////////////////////////////////////////////////////////////////

	class RemovableMsnObject {

		private MsnObject msnObject;

		private long lastAccessedTime;

		/**
		 * Create a new instance of the removable object.
		 * 
		 * @param msnObject Instance of the object.
		 */
		public RemovableMsnObject(MsnObject msnObject) {
			this.msnObject = msnObject;
			this.lastAccessedTime = Calendar.getInstance().getTimeInMillis();
		}

        /**
         * Get last accessed time for this picture.
         * 
         * @return Instance of the last accessed time for this picture.
         */
		public long getLastAccessedTime() {
			return lastAccessedTime;
		}

		/**
		 * Get the instance of the MsnObject for this removable object.
		 * 
		 * @return Instance of the MsnObject.
		 */
		public MsnObject getMsnObject() {
			this.lastAccessedTime = Calendar.getInstance().getTimeInMillis();
			return msnObject;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	
	private class RemoveMsnObjectWorker implements Runnable {
		
		/**
		 * @see Runnable#run()
		 */
		public void run() {
            try {
                long now = Calendar.getInstance().getTimeInMillis();
                Iterator<Map.Entry<String, RemovableMsnObject>> iter = 
                	pictures.entrySet().iterator();
                while (iter.hasNext()) {
                	Map.Entry<String, RemovableMsnObject> entry = iter.next();
        			RemovableMsnObject pic = entry.getValue();
        			if (pic != null && 
        				(now - pic.getLastAccessedTime() > 1000*60*20)){
        				iter.remove();
          			}
        		}
            } catch (RuntimeException e) {
            	e.printStackTrace();
            }
        }
    }
	
}
