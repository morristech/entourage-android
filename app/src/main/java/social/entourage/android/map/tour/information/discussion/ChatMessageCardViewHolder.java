package social.entourage.android.map.tour.information.discussion;

import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import jp.wasabeef.picasso.transformations.CropCircleTransformation;
import social.entourage.android.R;
import social.entourage.android.api.model.ChatMessage;
import social.entourage.android.api.model.TimestampedObject;
import social.entourage.android.api.tape.Events;
import social.entourage.android.tools.BusProvider;

/**
 * Chat Message Card for Tour Information Screen
 */
public class ChatMessageCardViewHolder extends BaseCardViewHolder {

    private ImageView mUserPhotoView;
    private TextView mMessageView;

    private int userId = 0;

    public ChatMessageCardViewHolder(final View view) {
        super(view);
    }

    @Override
    protected void bindFields() {

        mUserPhotoView = (ImageView) itemView.findViewById(R.id.tic_chat_user_photo);
        mMessageView = (TextView) itemView.findViewById(R.id.tic_chat_message);

        mUserPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (userId == 0) return;
                BusProvider.getInstance().post(new Events.OnUserViewRequestedEvent(userId));
            }
        });

    }

    @Override
    public void populate(final TimestampedObject data) {
        populate((ChatMessage)data);
    }

    public void populate(ChatMessage chatMessage) {
        String avatarURL = chatMessage.getUserAvatarURL();
        if (avatarURL != null) {
            Picasso.with(itemView.getContext()).load(Uri.parse(avatarURL))
                    .transform(new CropCircleTransformation())
                    .into(mUserPhotoView);
        }

        mMessageView.setText(chatMessage.getContent());

        userId = chatMessage.getUserId();
    }

    public static int getLayoutResource() {
        return R.layout.tour_information_chat_message_others_card_view;
    }
}
