package social.entourage.android.poi;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.lang.Object;

import javax.inject.Inject;


import butterknife.ButterKnife;
import butterknife.InjectView;
import social.entourage.android.EntourageActivity;
import social.entourage.android.R;
import social.entourage.android.api.model.map.Poi;
import social.entourage.android.common.Constants;

public class ReadPoiActivity extends EntourageActivity {

    private Poi poi;

    @Inject
    ReadPoiPresenter presenter;

    @InjectView(R.id.textview_poi_name)
    TextView txtPoiName;
    @InjectView(R.id.textview_poi_description)
    TextView txtPoiDesc;
    @InjectView(R.id.button_poi_phone)
    Button btnPoiPhone;
    @InjectView(R.id.button_poi_mail)
    Button btnPoiMail;
    @InjectView(R.id.button_poi_web)
    Button btnPoiWeb;
    @InjectView(R.id.button_poi_address)
    Button btnPoiAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi_read);
        ButterKnife.inject(this);

        Bundle args = getIntent().getExtras();
        poi = (Poi) args.get(Constants.KEY_POI);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        presenter.displayPoi(poi);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected List<Object> getScopedModules() {
        return Arrays.<Object>asList(new ReadPoiModule(this));
    }

    public void displayPoi(Poi poi, final ReadPoiPresenter.OnAddressClickListener onAddressClickListener) {
        txtPoiName.setText(poi.getName());
        txtPoiDesc.setText(poi.getDescription());
        setActionButton(btnPoiPhone, poi.getPhone());
        setActionButton(btnPoiMail, poi.getEmail());
        setActionButton(btnPoiWeb, poi.getWebsite());
        setActionButton(btnPoiAddress, poi.getAdress());
        btnPoiAddress.setOnClickListener(onAddressClickListener);
    }

    public void setActionButton(Button btn, String value) {
        if(value.length() > 0) {
            btn.setVisibility(View.VISIBLE);
            btn.setText(value);
        }
    }
}