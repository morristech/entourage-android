package social.entourage.android.newsfeed;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import social.entourage.android.Constants;
import social.entourage.android.EntourageEvents;
import social.entourage.android.R;
import social.entourage.android.api.model.Partner;
import social.entourage.android.api.model.TimestampedObject;
import social.entourage.android.api.model.map.Entourage;
import social.entourage.android.api.model.map.FeedItem;
import social.entourage.android.api.model.map.LastMessage;
import social.entourage.android.api.model.map.Tour;
import social.entourage.android.api.model.map.TourAuthor;
import social.entourage.android.api.model.map.TourPoint;
import social.entourage.android.api.tape.Events;
import social.entourage.android.base.BaseCardViewHolder;
import social.entourage.android.map.entourage.category.EntourageCategory;
import social.entourage.android.map.entourage.category.EntourageCategoryManager;
import social.entourage.android.tools.BusProvider;
import social.entourage.android.tools.CropCircleTransformation;
import social.entourage.android.view.PartnerLogoImageView;

/**
 * Created by mihaiionescu on 24/03/2017.
 */

public class FeedItemViewHolder extends BaseCardViewHolder {

    private TextView tourTitle;
    private ImageView photoView;
    private PartnerLogoImageView partnerLogoView;
    private TextView tourTypeTextView;
    private TextView tourAuthor;
    private TextView tourLocation;
    private TextView badgeCountView;
    private TextView numberOfPeopleTextView;
    private Button actButton;
    private View dividerLeft;
    private View dividerRight;
    private TextView lastMessageTextView;

    private FeedItem feedItem;

    private Context context;

    private OnClickListener onClickListener;

    public FeedItemViewHolder(final View itemView) {
        super(itemView);
    }

    @Override
    protected void bindFields() {

        tourTitle = (TextView)itemView.findViewById(R.id.tour_card_title);
        photoView = (ImageView)itemView.findViewById(R.id.tour_card_photo);
        partnerLogoView = (PartnerLogoImageView) itemView.findViewById(R.id.tour_card_partner_logo);
        tourTypeTextView = (TextView)itemView.findViewById(R.id.tour_card_type);
        tourAuthor = (TextView)itemView.findViewById(R.id.tour_card_author);
        tourLocation = (TextView)itemView.findViewById(R.id.tour_card_location);
        badgeCountView = (TextView)itemView.findViewById(R.id.tour_card_badge_count);
        numberOfPeopleTextView = (TextView)itemView.findViewById(R.id.tour_card_people_count);
        actButton = (Button)itemView.findViewById(R.id.tour_card_button_act);
        dividerLeft = itemView.findViewById(R.id.tour_card_divider_left);
        dividerRight = itemView.findViewById(R.id.tour_card_divider_right);
        lastMessageTextView = (TextView)itemView.findViewById(R.id.tour_card_last_message);

        onClickListener = new OnClickListener();

        itemView.setOnClickListener(onClickListener);
        //tourAuthor.setOnClickListener(onClickListener);
        if (photoView != null) photoView.setOnClickListener(onClickListener);
        if (actButton != null) actButton.setOnClickListener(onClickListener);

        context = itemView.getContext();
    }

    public static int getLayoutResource() {
        return R.layout.layout_tour_card;
    }

    @Override
    public void populate(final TimestampedObject data) {
        populate((FeedItem) data);
    }

    public void populate(FeedItem feedItem) {

        this.feedItem = feedItem;

        //configure the cell fields
        Resources res = itemView.getResources();

        //title
        if (tourTitle != null) {
            tourTitle.setText(String.format(res.getString(R.string.tour_cell_title), feedItem.getTitle()));
            if (showCategoryIcon()) {
                // add the icon for entourages
                if (feedItem.getType() == TimestampedObject.ENTOURAGE_CARD) {
                    EntourageCategory entourageCategory = EntourageCategoryManager.getInstance().findCategory((Entourage) feedItem);
                    Drawable categoryIcon = AppCompatResources.getDrawable(context, entourageCategory.getIconRes()).mutate();
                    categoryIcon.clearColorFilter();
                    categoryIcon.setColorFilter(ContextCompat.getColor(context, entourageCategory.getTypeColorRes()), PorterDuff.Mode.SRC_IN);
                    tourTitle.setCompoundDrawablesWithIntrinsicBounds(categoryIcon, null, null, null);
                } else {
                    Drawable categoryIcon = AppCompatResources.getDrawable(context, ((Tour) feedItem).getIconRes());
                    tourTitle.setCompoundDrawablesWithIntrinsicBounds(categoryIcon, null, null, null);
                }
            }
        }

        TourAuthor author = feedItem.getAuthor();
        if (author == null) {
            //author
            if (tourAuthor != null) {
                tourAuthor.setText("--");
            }
            if (photoView != null) {
                photoView.setImageResource(R.drawable.ic_user_photo_small);
            }
        } else {
            //author photo
            if (photoView != null) {
                String avatarURLAsString = author.getAvatarURLAsString();
                if (avatarURLAsString != null) {
                    Picasso.with(itemView.getContext())
                            .load(Uri.parse(avatarURLAsString))
                            .placeholder(R.drawable.ic_user_photo_small)
                            .transform(new CropCircleTransformation())
                            .into(photoView);
                } else {
                    photoView.setImageResource(R.drawable.ic_user_photo_small);
                }
            }
            // Partner logo
            if (partnerLogoView != null) {
                Partner partner = author.getPartner();
                if (partner != null) {
                    String partnerLogoURL = partner.getSmallLogoUrl();
                    if (partnerLogoURL != null) {
                        Picasso.with(itemView.getContext())
                                .load(Uri.parse(partnerLogoURL))
                                .placeholder(R.drawable.partner_placeholder)
                                .transform(new CropCircleTransformation())
                                .into(partnerLogoView);
                    } else {
                        partnerLogoView.setImageDrawable(null);
                    }
                } else {
                    partnerLogoView.setImageDrawable(null);
                }
            }

            //author
            if (tourAuthor != null) {
                tourAuthor.setText(String.format(res.getString(R.string.tour_cell_author), feedItem.getAuthor().getUserName()));
            }
        }

        //Feed Item type
        if (tourTypeTextView != null) {
            tourTypeTextView.setText(feedItem.getFeedTypeLong(context));
        }

        if (tourLocation != null) {
            String distanceAsString = "";
            TourPoint startPoint = feedItem.getStartPoint();
            if (startPoint != null) {
                distanceAsString = startPoint.distanceToCurrentLocation();
            }

            if (distanceAsString.equalsIgnoreCase("")) {
                tourLocation.setText(String.format(res.getString(R.string.tour_cell_location_no_distance), Tour.getStringDiffToNow(feedItem.getStartTime())));
            } else {
                tourLocation.setText(String.format(res.getString(R.string.tour_cell_location), Tour.getStringDiffToNow(feedItem.getStartTime()), distanceAsString));
            }
        }

        //tour members
        if (numberOfPeopleTextView != null) {
            numberOfPeopleTextView.setText(res.getString(R.string.tour_cell_numberOfPeople, feedItem.getNumberOfPeople()));
        }

        //badge count
        if (badgeCountView != null) {
            int badgeCount = feedItem.getBadgeCount();
            if (badgeCount <= 0) {
                badgeCountView.setVisibility(View.GONE);
            } else {
                badgeCountView.setVisibility(View.VISIBLE);
                badgeCountView.setText(res.getString(R.string.badge_count_format, feedItem.getBadgeCount()));
            }
        }

        //act button
        if (actButton != null) {
            int dividerColor = R.color.accent;
            int textColor = R.color.accent;
            actButton.setVisibility(View.VISIBLE);
            actButton.setPadding(0, 0, 0, 0);
            if (Build.VERSION.SDK_INT >= 16) {
                actButton.setPaddingRelative(0, 0, 0, 0);
            }
            if (feedItem.isFreezed()) {
                actButton.setText(R.string.tour_cell_button_freezed);
                actButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                dividerColor = R.color.greyish;
                textColor = R.color.greyish;
            } else {
                String joinStatus = feedItem.getJoinStatus();
                if (Tour.JOIN_STATUS_PENDING.equals(joinStatus)) {
                    actButton.setText(R.string.tour_cell_button_pending);
                    actButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                } else if (Tour.JOIN_STATUS_ACCEPTED.equals(joinStatus)) {
                    actButton.setPadding(0, 0, res.getDimensionPixelOffset(R.dimen.act_button_right_padding), 0);
                    if (Build.VERSION.SDK_INT >= 16) {
                        actButton.setPaddingRelative(0, 0, res.getDimensionPixelOffset(R.dimen.act_button_right_padding), 0);
                    }
                    if (feedItem.getAuthor() != null) {
                        if (feedItem.getType() == TimestampedObject.TOUR_CARD && feedItem.isOngoing()) {
                            actButton.setText(R.string.tour_cell_button_ongoing);
                        } else {
                            actButton.setText(R.string.tour_cell_button_accepted);
                        }
                    } else {
                        actButton.setText(R.string.tour_cell_button_accepted);
                    }
                    actButton.setCompoundDrawablesWithIntrinsicBounds(res.getDrawable(R.drawable.button_act_accepted), null, null, null);
                } else if (Tour.JOIN_STATUS_REJECTED.equals(joinStatus)) {
                    actButton.setText(R.string.tour_cell_button_rejected);
                    actButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                    textColor = R.color.tomato;
                } else {
                    actButton.setText(R.string.tour_cell_button_view);
                    actButton.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
                    dividerColor = R.color.greyish;
                    textColor = R.color.greyish_brown;
                }
            }
            actButton.setTextColor(res.getColor(textColor));

            dividerLeft.setBackgroundResource(dividerColor);
            dividerRight.setBackgroundResource(dividerColor);
        }

        //last message
        if (lastMessageTextView != null) {
            LastMessage lastMessage = feedItem.getLastMessage();
            if (lastMessage != null) {
                lastMessageTextView.setText(lastMessage.getText());
            } else {
                lastMessageTextView.setText("");
            }
            lastMessageTextView.setTypeface(null, feedItem.getBadgeCount() == 0 ? Typeface.NORMAL : Typeface.BOLD);
            lastMessageTextView.setTextColor(feedItem.getBadgeCount() == 0 ? res.getColor(R.color.greyish) : res.getColor(R.color.black));
        }

    }

    protected boolean showCategoryIcon() {
        return true;
    }

    //--------------------------
    // INNER CLASSES
    //--------------------------

    private class OnClickListener implements View.OnClickListener {

        @Override
        public void onClick(final View v) {
            if (feedItem == null) return;
            if (v == photoView || v == tourAuthor) {
                if (feedItem.getAuthor() != null) {
                    BusProvider.getInstance().post(new Events.OnUserViewRequestedEvent(feedItem.getAuthor().getUserID()));
                }
            }
            else if (v == actButton) {
                String joinStatus = feedItem.getJoinStatus();
                if (Tour.JOIN_STATUS_PENDING.equals(joinStatus)) {
                    EntourageEvents.logEvent(Constants.EVENT_FEED_PENDING_OVERLAY);
                    BusProvider.getInstance().post(new Events.OnFeedItemCloseRequestEvent(feedItem));
                } else if (Tour.JOIN_STATUS_ACCEPTED.equals(joinStatus)) {
                    EntourageEvents.logEvent(Constants.EVENT_FEED_OPEN_ACTIVE_OVERLAY);
                    BusProvider.getInstance().post(new Events.OnFeedItemCloseRequestEvent(feedItem));
                } else if (Tour.JOIN_STATUS_REJECTED.equals(joinStatus)) {
                    //What to do on rejected status ?
                } else {
                    // The server wants the position starting with 1
                    BusProvider.getInstance().post(new Events.OnFeedItemInfoViewRequestedEvent(feedItem, getAdapterPosition()+1));
                }

            }
            else if (v == itemView) {
                if (viewHolderListener != null) {
                    viewHolderListener.onViewHolderDetailsClicked(0);
                }
                // The server wants the position starting with 1
                BusProvider.getInstance().post(new Events.OnFeedItemInfoViewRequestedEvent(feedItem, getAdapterPosition()+1));
            }
        }

    }

}
