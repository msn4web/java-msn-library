package net.sf.jml;

// JRE io

import net.sf.jml.exception.JmlException;
import net.sf.jml.util.DigestUtils;
import net.sf.jml.util.StringUtils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;

/**
 * This class represents a MsnObject from the MSN protocol.
 * 
 * @author Angel Barragán Chacón
 */
public final class MsnObject {

	/**
	 * Type for custom emoticons.
	 */
    public static final int TYPE_CUSTOM_EMOTICON = 2;
    
    /**
     * Type for siaplay picture.
     */
    public static final int TYPE_DISPLAY_PICTURE = 3;
    
    /**
     * Type for background.
     */
    public static final int TYPE_BACKGROUND = 5;
    
    /**
     * Type for dinamic display picture.
     */
    public static final int TYPE_DYNAMIC_DISPLAY_PICTURE = 7;
    
    /**
     * Type for winks.
     */
    public static final int TYPE_WINKS = 8;
    
    /**
     * Type for voice clips.
     */
    public static final int TYPE_VOICE_CLIP = 11;
    
    /**
     * Type for Add in saved state.
     */
    public static final int TYPE_ADDIN_SAVED_STATE = 12;
    
    /**
     * Type for Location
     */
    public static final int TYPE_MSNP15_LOCATION = 14;

    ////////////////////////////////////////////////////////////////////////////

    /**
     * Create an instance of MsnObject for a display picture.
     * 
     * @param creator Creator of the MsnObject. 
     * @param picture Data for the MsnObject.
     * @return Instance of the MsnObject. 
     * @throws JmlException If an error happens.
     */
    public static MsnObject getInstance(String creator, byte picture[]) 
    throws JmlException{
        if (creator == null)throw new JmlException(
                "Creator can't null!");
        if (picture == null)throw new JmlException(
                "Picture can't null!");
        return new MsnObject(creator,picture);
    }

    /**
     * Create an instance of MsnObject for a display picture.
     * 
     * @param creator Creator of the MsnObject. 
     * @param pictureFileName Filename where the picture object is stored.
     * @return Instance of the MsnObject. 
     * @throws JmlException If an error happens.
     */
    public static MsnObject getInstance(String creator, String pictureFileName) throws JmlException {
        byte[] pic;
        try {
            RandomAccessFile msnObjFile = new RandomAccessFile(pictureFileName,
                    "r");
            pic = new byte[(int) msnObjFile.length()];
            msnObjFile.readFully(pic);
            msnObjFile.close();
        } catch (FileNotFoundException ex) {
            throw new JmlException(
                    "File " + pictureFileName + " not found!",ex);
        } catch (IOException ex) {
            throw new JmlException(
                    "File " + pictureFileName + " can't access!",ex);
        }
        return getInstance(creator,pic);
    }

	/**
	 * Create a MsnObject instance from its representation as XML.
	 * 
	 * @param msnObject XML representation of the MsnObject.
	 * @return Instance of the MsnObject.
	 */
	public static MsnObject parseMsnObject(String msnObject) {
		
		// Check the incoming data
		if (msnObject == null) {
			return null;
		}
		if (msnObject.trim().length() == 0) {
			return null;
		}

		try {
			// Create an instance of the MsnObject
			MsnObject instance = new MsnObject();
			
			// Remove first and end XML parts
			msnObject = msnObject.substring(8, msnObject.length() - 2);
	
			// Get the tag name
			int begin = 0;
			int end = 0;
			
			// Iterate looking for attributes
			while (end < msnObject.length()) {
			
				// Check the end of the attribute name
				for (; msnObject.charAt(end) != '=' ; end++);
				
				// Get attribute name
				String attributeName = msnObject.substring(begin, end);
				
				// Get string delimiter
				char delimiter = msnObject.charAt(end + 1);
				
				// Look for the other delimiter
				end = begin = end + 2;
				for (; msnObject.charAt(end) != delimiter ; end++);
				
				// Get the attribute value
				String attributeValue = msnObject.substring(begin, end);
				
				// Add attribute to the instance
				if (attributeName.equalsIgnoreCase("Creator")) {
					instance.creator = attributeValue;
				}
				else if (attributeName.equalsIgnoreCase("Size")) {
					instance.size = Long.parseLong(attributeValue);
				}
				else if (attributeName.equalsIgnoreCase("Type")) {
					instance.type = Integer.parseInt(attributeValue);
				}
				else if (attributeName.equalsIgnoreCase("Location")) {
					instance.location = attributeValue;
				}
				else if (attributeName.equalsIgnoreCase("Friendly")) {
					instance.friendly = attributeValue;
				}
				else if (attributeName.equalsIgnoreCase("SHA1D")) {
					instance.sha1d = attributeValue;
				}
				else if (attributeName.equalsIgnoreCase("SHA1C")) {
					instance.sha1c = attributeValue;
				}
	
				// Check the begining of the attribute name
				begin = end + 1;
				for (; 
					begin < msnObject.length() && 
				    Character.isWhitespace(msnObject.charAt(begin)) ; begin++);
				end = begin;
			}
			
			// Generate sha1c if it is empty
			if (instance.sha1c.length() == 0) {
				instance.generate();
			}
			
			// Retruen the instance
			return instance;
			
		} catch (Exception e) {
			
			// If the string is not in the expected format return null
			return null;
		}
		
	}
	
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Create an empty MsnObject.
     */
    private MsnObject() {
    }
    
    /**
     * Create a local MsnObject fiven it's creator an the data for the object.
     * 
     * @param creator Creator for the MsnObject.
     * @param msnObj Data for the MsnObject.
     */
    private MsnObject(String creator, byte msnObj[]) {
        this.creator = creator;
        this.msnObj = msnObj;
        this.size = msnObj.length;
        generate();
    }

    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Creator for the MsnObject.
     */
    private String creator = "";

    /**
     * Retrieve the creator for this MsnObject.
     * 
     * @return Cretor for this MsnObject.
     */
    public String getCreator() {
        return creator;
    }

    /**
     * Sets the creator for this MsnObject.
     * 
     * @param creator New creator for this MsnObject.
     */
    public void setCreator(String creator) {
        this.creator = creator;
        generate();
    }
    
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Size of the MsnObject data.
     */
    private long size = 0;

    /**
     * Retrieves the size of the MsnObject data.
     * 
     * @return MsnObject data size.
     */
    public long getSize() {
        return size;
    }
    
	/**
	 * Sets the new size for the MsnObject.
	 * 
	 * @param size the size to set Size for the data of the MsnObject.
	 */
	public void setSize(long size) {
		this.size = size;
        generate();
	}
    
    ////////////////////////////////////////////////////////////////////////////

	/**
	 * Type for the MsnObject.
	 */
	private int type = TYPE_DISPLAY_PICTURE;

	/**
	 * Retrieves the type for the MsnObject.
	 * 
	 * @return Type for the MsnObject.
	 */
    public int getType() {
        return type;
    }

    /**
     * Sets the type for this MsnObject.
     * 
     * @param type Type for the MsnObject.
     */
    public void setType(int type) {
        this.type = type;
        generate();
    }
	
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Location for this MsnObject.
     */
    private String location = "0";

    /**
     * Retrieves the location for this MsnObject.
     * 
     * @return Location for this MsnObject.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location for this MsnObject.
     * 
     * @param location Location for this MsnObject.
     */
    public void setLocation(String location) {
        this.location = location;
        generate();
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Friendly name for the MsnObject.
     */
    private String friendly = "AAA=";

    /**
     * Retrieves the friendly name of the MsnObject.
     * 
     * @return Friendly name for the MsnObject.
     */
    public String getFriendly() {
        return friendly;
    }

    /**
     * Sets the friendly name for the MsnObject.
     * 
     * @param friendly New Friendly name.
     */
    public void setFriendly(String friendly) {
        if (friendly == null) return;
        try {
            friendly = StringUtils.encodeBase64(friendly.getBytes("UTF-16BE"));
        } catch (UnsupportedEncodingException ex) {
            friendly = StringUtils.encodeBase64(friendly.getBytes());
        }
        this.friendly = friendly;
        generate();
    }
    
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * SHA for the MsnObject data.
     */
    private String sha1d = "";

    /**
     * Retrieves the SHA of the MsnObject data.
     * 
     * @return SHA value.
     */
    public String getSha1d() {
        return sha1d;
    }

	/**
	 * Sets the new SHA value for the MsnObject data.
	 * 
	 * @param sha1d the sha1d to set.
	 */
	public void setSha1d(String sha1d) {
		this.sha1d = sha1d;
	}
    
    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * SHA for the MsnObject properties data.
     */
    private String sha1c = "";

    /**
     * Retrieves the SHA value for the MsnObject properties data.
     * 
     * @return SHA value.
     */
    public String getSha1c() {
        return sha1c;
    }

	/**
	 * Sets the new SHA value for the MsnObject properties data.
	 * 
	 * @param sha1c the sha1c to set.
	 */
	public void setSha1c(String sha1c) {
		this.sha1c = sha1c;
	}

    ////////////////////////////////////////////////////////////////////////////
    
    /**
     * Content for local MsnObjects.
     */
    private byte msnObj[];

    /**
     * Retrieves the MsnObject local data.
     * 
     * @return MsnObject local data.
     */
    public byte[] getMsnObj() {
        return msnObj;
    }
    
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Generate the SHA value for the MsnObject properties data.
     */
    private void generate() {
    	if (msnObj != null) {
    		sha1d = StringUtils.encodeBase64(DigestUtils.sha1(msnObj));
    	}
        String tmpSha1c = "Creator" + getCreator() + "Size" + size +
                          "Type" + this.getType() + "Location" + getLocation() +
                          "Friendly" +
                          this.getFriendly() + "SHA1D" + sha1d;
        sha1c = StringUtils.encodeBase64(DigestUtils.sha1(tmpSha1c.getBytes()));
    }

    /**
     * @see Object#toString()
     */
    @Override
	public String toString() {
        StringBuffer ret = new StringBuffer("<msnobj Creator=");
        ret.append("\"").append(this.getCreator()).append("\"");
        ret.append(" Size=");
        ret.append("\"").append(this.getSize()).append("\"");
        ret.append(" Type=");
        ret.append("\"").append(this.getType()).append("\"");
        ret.append(" Location=");
        ret.append("\"").append(this.getLocation()).append("\"");
        ret.append(" Friendly=");
        ret.append("\"").append(this.getFriendly()).append("\"");
        ret.append(" SHA1D=");
        ret.append("\"").append(this.getSha1d()).append("\"");
        ret.append(" SHA1C=");
        ret.append("\"").append(this.getSha1c()).append("\"");
        ret.append("/>");
        return ret.toString();
    }

    /**
     * @see Object#equals(Object)
     */
    @Override
	public boolean equals(Object object) {
        if (getSha1c() == null) return false;

        if (this == object) {
            return true;
        }

        if (object == null || !(object instanceof MsnObject)) {
        	return false;
        }
        return getSha1c().equals(((MsnObject) object).getSha1c());
    }

    @Override
    public int hashCode() {
    	if (getSha1c() == null) {
    		return 0;
    	}
    	
    	return getSha1c().hashCode();
    }
}
