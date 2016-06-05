package com.jonathanblood.restaurant.viewholder;

import android.content.Context;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.jonathanblood.restaurant.R;
import com.jonathanblood.restaurant.models.Restaurant;
import com.jonathanblood.restaurant.picasso.CircleTransformation;
import com.squareup.picasso.Picasso;

/**
 * Created by Jonat on 04/06/2016.
 */
public class RestaurantViewHolder extends RecyclerView.ViewHolder
{
    public TextView nameTextView;
    public TextView createdByTextView;
    public TextView ratingTextView;
    public ImageView pictureImageView;

    public RestaurantViewHolder(View itemView)
    {
        super(itemView);
        nameTextView = (TextView) itemView.findViewById(R.id.name_text_view);
        createdByTextView = (TextView) itemView.findViewById(R.id.created_by_text_view);
        ratingTextView = (TextView) itemView.findViewById(R.id.rating_text_view);
        pictureImageView = (ImageView) itemView.findViewById(R.id.restaurant_image);
    }

    public void bindToRestaurant(Context context, Restaurant restaurant, String prefix, String none) {
        nameTextView.setText(restaurant.getName());
        createdByTextView.setText(prefix + " " + restaurant.getCreatedBy());
        String rating = restaurant.getRating();
        if (none.equalsIgnoreCase(rating))
        {
            rating = "-";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ratingTextView.setBackgroundColor(context.getColor(R.color.background));
            else
                ratingTextView.setBackgroundColor(context.getResources().getColor(R.color.background));
        } else
        {
            setScoreColour(context, rating);
        }
        ratingTextView.setText(rating);
    }

    private void setScoreColour(Context context, String scoreStr)
    {
        int score = Integer.parseInt(scoreStr);
        if (score <= 4)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ratingTextView.setBackgroundColor(context.getColor(R.color.score_red));
            else
                ratingTextView.setBackgroundColor(context.getResources().getColor(R.color.score_red));
        } else if (score >= 5 && score <= 8)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ratingTextView.setBackgroundColor(context.getColor(R.color.score_yellow));
            else
                ratingTextView.setBackgroundColor(context.getResources().getColor(R.color.score_yellow));
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                ratingTextView.setBackgroundColor(context.getColor(R.color.score_green));
            else
                ratingTextView.setBackgroundColor(context.getResources().getColor(R.color.score_green));
        }
    }

}
