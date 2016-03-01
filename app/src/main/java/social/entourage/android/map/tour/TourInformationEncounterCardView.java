package social.entourage.android.map.tour;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.text.TextPaint;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import social.entourage.android.R;
import social.entourage.android.api.model.map.Encounter;

/**
 * Encounter Card View
 */
public class TourInformationEncounterCardView extends LinearLayout {

    private TextView mStreetPersonNameView;
    private TextView mMessageView;

    private Context context;

    private boolean addressRetrieved = false;

    public TourInformationEncounterCardView(Context context) {
        super(context, null, R.attr.TourInformationEncounterCardViewStyle);
        init(null, 0);
    }

    public TourInformationEncounterCardView(Context context, AttributeSet attrs) {
        super(context, attrs, R.attr.TourInformationEncounterCardViewStyle);
        init(attrs, 0);
    }

    public TourInformationEncounterCardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        context = getContext();
        inflate(context, R.layout.tour_information_encounter_card_view, this);

        mStreetPersonNameView = (TextView)findViewById(R.id.tic_encounter_street_name);
        mMessageView = (TextView)findViewById(R.id.tic_encounter_message);
    }

    public void populate(Encounter encounter) {
        String location = "";
        Address address = encounter.getAddress();
        if (address != null) {
            if (address.getMaxAddressLineIndex() > 0) {
                location = address.getAddressLine(0);
            }
        }
        else {
            if (!addressRetrieved) {
                new GeocoderTask().execute(encounter);
            }
        }
        String encounterDate = "";
        if (encounter.getCreationDate() != null) {
            encounterDate = DateFormat.getDateFormat(context).format(encounter.getCreationDate());
        }
        String encounterLocation = getResources().getString(R.string.encounter_read_location,
                encounter.getUserName(),
                encounter.getStreetPersonName(),
                location,
                encounterDate);
        mStreetPersonNameView.setText(encounterLocation);
        mMessageView.setText(encounter.getMessage());
    }

    private class GeocoderTask extends AsyncTask<Encounter, Void, Encounter> {

        @Override
        protected Encounter doInBackground(final Encounter... params) {
            try {
                Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
                Encounter encounter = params[0];
                List<Address> addresses = geoCoder.getFromLocation(encounter.getLatitude(), encounter.getLongitude(), 1);
                if (addresses.size() > 0) {
                    encounter.setAddress(addresses.get(0));
                }
                return encounter;
            }
            catch (IOException e) {

            }
            return null;
        }

        @Override
        protected void onPostExecute(final Encounter encounter) {
            addressRetrieved = true;
            populate(encounter);
        }
    }
}