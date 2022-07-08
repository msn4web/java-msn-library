package net.sf.jml;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Contact en attente
 * 
 * @author Jessou
 */
public class MsnContactPending{

    private Email email;
    private String displayName;
    private Date joinedDate;

    /**
     * @param email
     * @param displayName
     * @param joinedDate
     */
    public MsnContactPending(Email email,String displayName,Date joinedDate){
        super();
        this.email = email;
        this.displayName = displayName;
        this.joinedDate = joinedDate;
    }

    /**
     * @return the email
     */
    public Email getEmail(){
        return email;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName(){
        return displayName;
    }

    /**
     * @return the joinedDate
     */
    public Date getJoinedDate(){
        return joinedDate;
    }


    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss");

    @Override
    public String toString(){
        return "email : " + email.getEmailAddress() + "- displayName : " + displayName + "- joinedDate : "
        + sdf.format(joinedDate);
    }
}
