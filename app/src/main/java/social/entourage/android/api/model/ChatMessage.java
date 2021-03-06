package social.entourage.android.api.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Created by mihaiionescu on 25/02/16.
 */
public class ChatMessage extends TimestampedObject implements Serializable {

    private final static String HASH_STRING_HEAD = "ChatMessage-";
    private static final long serialVersionUID = 2171009008739523540L;

    @Expose(serialize = false)
    @SerializedName("id")
    private long chatId;

    private String content;

    @Expose(serialize = false)
    @SerializedName("created_at")
    private Date creationDate;

    @Expose(serialize = false)
    private User user;

    @Expose(serialize = false, deserialize = false)
    private boolean isMe;

    // ----------------------------------
    // CONSTRUCTORS
    // ----------------------------------

    public ChatMessage(String message) {
        this.content = message;
    }

    // ----------------------------------
    // GETTERS AND SETTERS
    // ----------------------------------

    public long getChatId() {
        return chatId;
    }

    public void setChatId(final long chatId) {
        this.chatId = chatId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    public int getUserId() {
        if (user != null) {
            return user.getId();
        }
        return 0;
    }

    public String getUserAvatarURL() {
        if (user == null) return null;
        return user.getAvatarURL();
    }

    public String getUserName() {
        if (user == null) return "";
        return user.getDisplayName();
    }

    public String getPartnerLogoSmall() {
        if (user == null || user.getPartner() == null) return null;
        return user.getPartner().getSmallLogoUrl();
    }

    public boolean isMe() {
        return isMe;
    }

    public void setIsMe(final boolean isMe) {
        this.isMe = isMe;
    }

    @Override
    public Date getTimestamp() {
        return creationDate;
    }

    @Override
    public String hashString() {
        return HASH_STRING_HEAD + chatId;
    }

    @Override
    public boolean equals(final Object o) {
        return !(o == null || o.getClass() != this.getClass()) && this.chatId == ((ChatMessage) o).chatId;
    }

    @Override
    public int getType() {
        return isMe ? CHAT_MESSAGE_ME : CHAT_MESSAGE_OTHER;
    }

    @Override
    public long getId() {
        return chatId;
    }

    // ----------------------------------
    // WRAPPERS
    // ----------------------------------

    public static class ChatMessageWrapper {

        @SerializedName("chat_message")
        private ChatMessage chatMessage;

        public ChatMessage getChatMessage() {
            return chatMessage;
        }

        public void setChatMessage(final ChatMessage chatMessage) {
            this.chatMessage = chatMessage;
        }

    }

    public static class ChatMessagesWrapper {

        @SerializedName("chat_messages")
        private List<ChatMessage> chatMessages;

        public List<ChatMessage> getChatMessages() {
            return chatMessages;
        }

        public void setChatMessages(List<ChatMessage> chatMessages) {
            this.chatMessages = chatMessages;
        }

    }

}
