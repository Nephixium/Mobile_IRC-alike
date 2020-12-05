package com.android.messaging;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


// Hanterar meddelanden som tas emot av klienten och uppdaterar RecyclerView objektet genom att utöka det med allt fler pratbubblor utifrån meddelandenas innehåll.
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MyViewHolder> {

    private ArrayList<String> messageList;

    // Construtctorn innehåller datamängden som information ska hämtas från för att uppdatera och lägga till objekt i RecyclerView.
    public MessageAdapter(ArrayList<String> messageArray) {
        this.messageList = messageArray;
    }

    // Skapar ett nytt ViewHolder objekt utifrån layout filen list_message_layout.
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View messageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_message_layout, parent, false);
        return new MyViewHolder(messageView);
    }

    // Hämtar meddelandet ur messageList, ger färg på användarnamnet genom att använda SpannableString och ange färg samt stil, och applicerar det på textfältet messageText som kommer läggas till i RecyclerView listan av objekt.
    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String splitMessage[] = messageList.get(position).split("says:");

        SpannableStringBuilder builder = new SpannableStringBuilder();
        SpannableString formattedUsername = new SpannableString(splitMessage[0]);
        formattedUsername.setSpan(new ForegroundColorSpan(0xFF11FF11), 0, splitMessage[0].length(), 0);
        StyleSpan bold = new StyleSpan(Typeface.BOLD);
        formattedUsername.setSpan(bold, 0, splitMessage[0].length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        builder.append(formattedUsername);
        builder.append(splitMessage[1]);

        holder.messageText.setText(builder, TextView.BufferType.SPANNABLE);
    }

    // Återger mängden i datasamlingen messageList.
    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ViewHolder som innehåller det textfält som skall läggas till i RecyclerView objektet.
    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView messageText;

        public MyViewHolder(View view) {
            super(view);
            this.messageText = view.findViewById(R.id.message_layout_object);
        }
    }
}
