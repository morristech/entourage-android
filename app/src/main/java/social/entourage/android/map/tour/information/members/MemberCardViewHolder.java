package social.entourage.android.map.tour.information.members;

import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;
import social.entourage.android.api.model.TimestampedObject;
import social.entourage.android.api.model.map.TourUser;
import social.entourage.android.api.tape.Events;
import social.entourage.android.base.BaseCardViewHolder;

import social.entourage.android.R;
import social.entourage.android.tools.BusProvider;

/**
 * Created by mihaiionescu on 23/05/16.
 */
public class MemberCardViewHolder extends BaseCardViewHolder {

    private int userId = 0;

    private ImageView mMemberPhoto;
    private TextView mMemberName;

    public MemberCardViewHolder(final View view) {
        super(view);
    }



    @Override
    protected void bindFields() {
        mMemberPhoto = (ImageView)itemView.findViewById(R.id.tic_member_photo);
        mMemberName = (TextView)itemView.findViewById(R.id.tic_member_name);

        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (userId == 0) return;
                BusProvider.getInstance().post(new Events.OnUserViewRequestedEvent(userId));
            }
        });
    }

    @Override
    public void populate(final TimestampedObject data) {
        this.populate((TourUser)data);
    }

    public void populate(TourUser tourUser) {
        this.userId = tourUser.getUserId();
        String avatarURL = tourUser.getAvatarURLAsString();
        if (avatarURL != null) {
            Picasso.with(itemView.getContext()).load(Uri.parse(avatarURL))
                    .transform(new CropCircleTransformation())
                    .into(mMemberPhoto);
        }
        mMemberName.setText(tourUser.getDisplayName());
    }

    public static int getLayoutResource() {
        return R.layout.tour_information_member_card;
    }
}