package edu.utep.cs.mypricewatcher.firebase;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseHelper extends pricewatcher.model.FirebaseHelper {

	private static FirebaseHelper theInstance;
		
    private FirebaseHelper() {
    }

	public static FirebaseHelper getInstance() {
        if (theInstance == null) {
        	theInstance = new FirebaseHelper();
        }
        return theInstance;
    }

    @Override
    protected void setOptions(FirebaseDatabase database) {
        database.setPersistenceEnabled(true); // not supported on Java
    }
}
