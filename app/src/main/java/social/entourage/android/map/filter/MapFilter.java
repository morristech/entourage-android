package social.entourage.android.map.filter;

import java.io.Serializable;

import social.entourage.android.api.model.TourType;
import social.entourage.android.api.model.map.Entourage;

/**
 * Created by mihaiionescu on 17/05/16.
 */
public class MapFilter implements Serializable {

    private static final long serialVersionUID = -2822136342813499636L;

    // ----------------------------------
    // Attributes
    // ----------------------------------

    public static final int DAYS_1 = 24; //hours
    public static final int DAYS_2 = 8*24; //hours
    public static final int DAYS_3 = 30*24; //hours

    public boolean tourTypeMedical = true;
    public boolean tourTypeSocial = true;
    public boolean tourTypeDistributive = true;

    public boolean entourageTypeDemand = true;
    public boolean entourageTypeContribution = true;

    public boolean showTours = true;

    public boolean onlyMyEntourages = false;
    public boolean onlyMyOrganisationEntourages = false;

    public int timeframe = DAYS_2;

    // ----------------------------------
    // Lifecycle
    // ----------------------------------

    private static MapFilter ourInstance = new MapFilter();

    public static MapFilter getInstance() {
        return ourInstance;
    }

    protected MapFilter() {
    }

    // ----------------------------------
    // Methods
    // ----------------------------------

    public String getTourTypes() {
        StringBuilder tourTypes = new StringBuilder("");
        if (tourTypeMedical) {
            tourTypes.append(TourType.MEDICAL.getName());
        }
        if (tourTypeSocial) {
            if (tourTypes.length() > 0) tourTypes.append(",");
            tourTypes.append(TourType.BARE_HANDS.getName());
        }
        if (tourTypeDistributive) {
            if (tourTypes.length() > 0) tourTypes.append(",");
            tourTypes.append(TourType.ALIMENTARY.getName());
        }

        return tourTypes.toString();
    }

    public String getEntourageTypes() {
        StringBuilder entourageTypes = new StringBuilder("");

        if (entourageTypeDemand) {
            entourageTypes.append(Entourage.TYPE_DEMAND);
        }
        if (entourageTypeContribution) {
            if (entourageTypes.length() > 0) entourageTypes.append(",");
            entourageTypes.append(Entourage.TYPE_CONTRIBUTION);
        }

        return entourageTypes.toString();
    }

}
