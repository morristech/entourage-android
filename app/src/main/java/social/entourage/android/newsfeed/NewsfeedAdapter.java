package social.entourage.android.newsfeed;

import social.entourage.android.base.EntourageBaseAdapter;
import social.entourage.android.api.model.TimestampedObject;
import social.entourage.android.map.entourage.EntourageViewHolder;
import social.entourage.android.map.tour.TourViewHolder;
import social.entourage.android.map.tour.information.discussion.ViewHolderFactory;

/**
 * Created by mihaiionescu on 05/05/16.
 */
public class NewsfeedAdapter extends EntourageBaseAdapter {

    public NewsfeedAdapter() {

        ViewHolderFactory.registerViewHolder(
                TimestampedObject.TOUR_CARD,
                new ViewHolderFactory.ViewHolderType(TourViewHolder.class, TourViewHolder.getLayoutResource())
        );

        ViewHolderFactory.registerViewHolder(
                TimestampedObject.ENTOURAGE_CARD,
                new ViewHolderFactory.ViewHolderType(EntourageViewHolder.class, EntourageViewHolder.getLayoutResource())
        );

        setHasStableIds(false);
    }
}